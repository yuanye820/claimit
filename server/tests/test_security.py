from datetime import datetime, timedelta, timezone

import jwt
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.auth import ALGORITHM, SECRET_KEY, create_access_token
from app.models import User


def register(client: TestClient, email: str = "alice@example.com"):
    return client.post(
        "/auth/register",
        json={
            "email": email,
            "password": "password123",
            "display_name": "Alice",
        },
    )


def test_data_endpoints_require_authentication(client: TestClient):
    assert client.post("/lists", json={"name": "Trip"}).status_code == 401
    assert (
        client.post(
            "/lists/1/items",
            json={"name": "Tent", "total": 1},
        ).status_code
        == 401
    )
    assert (
        client.post(
            "/items/1/claims",
            json={"quantity": 1},
        ).status_code
        == 401
    )
    assert client.get("/items/1/claims").status_code == 401


def test_invalid_token_is_rejected(client: TestClient):
    headers = {"Authorization": "Bearer not-a-token"}
    assert client.get("/auth/me", headers=headers).status_code == 401
    assert client.get("/items/1/claims", headers=headers).status_code == 401


def test_tampered_token_is_rejected(client: TestClient, auth_headers):
    token = auth_headers()["Authorization"].removeprefix("Bearer ")
    tampered = token[:-1] + ("A" if token[-1] != "A" else "B")

    response = client.get(
        "/auth/me",
        headers={"Authorization": f"Bearer {tampered}"},
    )

    assert response.status_code == 401


def test_expired_token_is_rejected(client: TestClient):
    now = datetime.now(timezone.utc)
    token = jwt.encode(
        {
            "sub": "1",
            "iat": now - timedelta(hours=2),
            "exp": now - timedelta(hours=1),
        },
        SECRET_KEY,
        algorithm=ALGORITHM,
    )

    response = client.get(
        "/auth/me",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 401


def test_token_signed_with_wrong_secret_is_rejected(client: TestClient):
    token = jwt.encode(
        {
            "sub": "1",
            "exp": datetime.now(timezone.utc) + timedelta(minutes=5),
        },
        "attacker-secret-that-is-32-bytes!!",
        algorithm=ALGORITHM,
    )

    response = client.get(
        "/auth/me",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 401


def test_unsigned_token_is_rejected(client: TestClient):
    token = jwt.encode(
        {
            "sub": "1",
            "exp": datetime.now(timezone.utc) + timedelta(minutes=5),
        },
        key="",
        algorithm="none",
    )

    response = client.get(
        "/auth/me",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 401


def test_token_for_unknown_user_is_rejected(client: TestClient):
    token = create_access_token(999_999)

    response = client.get(
        "/auth/me",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 401


def test_password_is_stored_as_a_salted_hash(
    client: TestClient,
    db_session,
):
    assert register(client).status_code == 201
    assert register(client, email="bob@example.com").status_code == 201

    users = db_session.execute(select(User).order_by(User.id)).scalars().all()

    assert len(users) == 2
    for user in users:
        assert user.password_hash.startswith("pbkdf2_sha256$200000$")
        assert "password123" not in user.password_hash
    assert users[0].password_hash != users[1].password_hash


def test_duplicate_email_is_case_insensitive(client: TestClient):
    assert register(client).status_code == 201

    response = register(client, email="ALICE@example.com")

    assert response.status_code == 409


def test_login_does_not_reveal_which_part_was_wrong(client: TestClient):
    assert register(client).status_code == 201

    wrong_password = client.post(
        "/auth/login",
        json={"email": "alice@example.com", "password": "wrong-password"},
    )
    unknown_email = client.post(
        "/auth/login",
        json={"email": "nobody@example.com", "password": "password123"},
    )

    assert wrong_password.status_code == unknown_email.status_code == 401
    assert wrong_password.json()["detail"] == unknown_email.json()["detail"]


def test_sql_injection_payload_is_stored_literally(
    client: TestClient,
    auth_headers,
):
    headers = auth_headers()
    payload = "'; DROP TABLE items;--"

    list_response = client.post("/lists", headers=headers, json={"name": "Trip"})
    list_id = list_response.json()["id"]
    item_response = client.post(
        f"/lists/{list_id}/items",
        headers=headers,
        json={"name": payload, "total": 1},
    )

    assert item_response.status_code == 201
    items = client.get(f"/lists/{list_id}/items").json()
    assert [item["name"] for item in items] == [payload]

    second = client.post(
        f"/lists/{list_id}/items",
        headers=headers,
        json={"name": "Stove", "total": 1},
    )
    assert second.status_code == 201


def test_cors_allows_only_the_configured_origin(client: TestClient):
    preflight = {
        "Origin": "http://localhost:8080",
        "Access-Control-Request-Method": "POST",
        "Access-Control-Request-Headers": "content-type,authorization",
    }
    allowed = client.options("/lists", headers=preflight)

    assert allowed.status_code == 200
    assert (
        allowed.headers.get("access-control-allow-origin")
        == "http://localhost:8080"
    )

    denied = client.options(
        "/lists",
        headers={**preflight, "Origin": "https://attacker.example"},
    )

    assert "access-control-allow-origin" not in denied.headers
