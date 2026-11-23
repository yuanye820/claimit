from fastapi.testclient import TestClient


def make_list(
    client: TestClient,
    headers: dict[str, str],
) -> int:
    response = client.post("/lists", headers=headers, json={"name": "Trip"})
    assert response.status_code == 201
    return response.json()["id"]


def test_list_items_returns_items_in_id_order(
    client: TestClient,
    auth_headers,
):
    headers = auth_headers()
    list_id = make_list(client, headers)
    for name in ("Tent", "Stove"):
        response = client.post(
            f"/lists/{list_id}/items",
            headers=headers,
            json={"name": name, "total": 1},
        )
        assert response.status_code == 201

    response = client.get(f"/lists/{list_id}/items")

    assert response.status_code == 200
    assert [item["name"] for item in response.json()] == ["Tent", "Stove"]


def test_list_items_unknown_list_returns_404(client: TestClient):
    response = client.get("/lists/999/items")
    assert response.status_code == 404
