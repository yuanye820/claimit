from fastapi.testclient import TestClient


def make_item(
    client: TestClient,
    headers: dict[str, str],
    total: int = 1,
) -> int:
    response = client.post("/lists", headers=headers, json={"name": "Trip"})
    assert response.status_code == 201
    list_id = response.json()["id"]

    response = client.post(
        f"/lists/{list_id}/items",
        headers=headers,
        json={"name": "Tent", "total": total},
    )
    assert response.status_code == 201
    return response.json()["id"]


def test_claim_last_unit_twice_one_conflicts(client: TestClient, auth_headers):
    alice = auth_headers("alice@example.com", display_name="Alice")
    bob = auth_headers("bob@example.com", display_name="Bob")
    item_id = make_item(client, alice, total=1)

    first = client.post(
        f"/items/{item_id}/claims",
        headers=alice,
        json={"quantity": 1},
    )
    second = client.post(
        f"/items/{item_id}/claims",
        headers=bob,
        json={"quantity": 1},
    )

    assert {first.status_code, second.status_code} == {200, 409}
    item = client.get(f"/items/{item_id}").json()
    assert item["remaining"] == 0


def test_claim_more_than_remaining_returns_409(client: TestClient, auth_headers):
    headers = auth_headers()
    item_id = make_item(client, headers, total=1)

    response = client.post(
        f"/items/{item_id}/claims",
        headers=headers,
        json={"quantity": 2},
    )

    assert response.status_code == 409
    assert client.get(f"/items/{item_id}").json()["remaining"] == 1


def test_release_restores_the_claimed_quantity(client: TestClient, auth_headers):
    headers = auth_headers()
    item_id = make_item(client, headers, total=2)

    claimed = client.post(
        f"/items/{item_id}/claims",
        headers=headers,
        json={"quantity": 1},
    )
    assert claimed.status_code == 200

    released = client.post(
        f"/items/{item_id}/releases",
        headers=headers,
        json={"quantity": 1},
    )
    assert released.status_code == 200
    assert released.json()["quantity"] == 0
    assert client.get(f"/items/{item_id}").json()["remaining"] == 2


def test_claim_history_returns_events(client: TestClient, auth_headers):
    headers = auth_headers("alice@example.com", display_name="Alice")
    item_id = make_item(client, headers, total=2)

    assert client.post(
        f"/items/{item_id}/claims",
        headers=headers,
        json={"quantity": 1},
    ).status_code == 200
    assert client.post(
        f"/items/{item_id}/releases",
        headers=headers,
        json={"quantity": 1},
    ).status_code == 200

    response = client.get(f"/items/{item_id}/claims", headers=headers)

    assert response.status_code == 200
    events = response.json()
    assert [(event["display_name"], event["delta"]) for event in events] == [
        ("Alice", 1),
        ("Alice", -1),
    ]
