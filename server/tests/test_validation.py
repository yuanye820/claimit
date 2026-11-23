from fastapi.testclient import TestClient


def make_item(client: TestClient) -> int:
    response = client.post("/lists", json={"name": "Trip"})
    assert response.status_code == 201
    list_id = response.json()["id"]

    response = client.post(
        f"/lists/{list_id}/items",
        json={"name": "Tent", "total": 1},
    )
    assert response.status_code == 201
    return response.json()["id"]


def test_claim_requires_bearer_token(client: TestClient):
    item_id = make_item(client)

    response = client.post(
        f"/items/{item_id}/claims",
        json={"quantity": 1},
    )

    assert response.status_code == 401


def test_claim_rejects_zero_quantity(client: TestClient, auth_headers):
    item_id = make_item(client)

    response = client.post(
        f"/items/{item_id}/claims",
        headers=auth_headers(),
        json={"quantity": 0},
    )

    assert response.status_code == 422


def test_claim_unknown_item_returns_404(client: TestClient, auth_headers):
    response = client.post(
        "/items/999/claims",
        headers=auth_headers(),
        json={"quantity": 1},
    )

    assert response.status_code == 404


def test_release_without_claim_returns_409(client: TestClient, auth_headers):
    item_id = make_item(client)

    response = client.post(
        f"/items/{item_id}/releases",
        headers=auth_headers(),
        json={"quantity": 1},
    )

    assert response.status_code == 409
