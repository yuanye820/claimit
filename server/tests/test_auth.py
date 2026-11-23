from fastapi.testclient import TestClient


def register(
    client: TestClient,
    email: str = "alice@example.com",
    password: str = "password123",
    display_name: str = "Alice",
):
    return client.post(
        "/auth/register",
        json={
            "email": email,
            "password": password,
            "display_name": display_name,
        },
    )


def test_register_returns_a_token_and_me(client: TestClient):
    response = register(client)
    assert response.status_code == 201

    token = response.json()["access_token"]
    me = client.get("/auth/me", headers={"Authorization": f"Bearer {token}"})

    assert me.status_code == 200
    assert me.json()["email"] == "alice@example.com"
    assert me.json()["display_name"] == "Alice"


def test_login_returns_a_token(client: TestClient):
    register(client)

    response = client.post(
        "/auth/login",
        json={"email": "alice@example.com", "password": "password123"},
    )

    assert response.status_code == 200
    assert response.json()["token_type"] == "bearer"
    assert response.json()["access_token"]


def test_duplicate_email_returns_409(client: TestClient):
    register(client)
    response = register(client, display_name="Another Alice")

    assert response.status_code == 409


def test_wrong_password_returns_401(client: TestClient):
    register(client)

    response = client.post(
        "/auth/login",
        json={"email": "alice@example.com", "password": "wrong-password"},
    )

    assert response.status_code == 401


def test_me_without_token_returns_401(client: TestClient):
    response = client.get("/auth/me")
    assert response.status_code == 401
