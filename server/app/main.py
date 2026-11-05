import secrets
from typing import Annotated

from fastapi import Depends, FastAPI, Header, HTTPException
from sqlalchemy import select, update
from sqlalchemy.orm import Session

from .db import get_db, init_db
from .models import Claim, Item, ItemList
from .schemas import ClaimOut, ClaimRequest, ItemCreate, ItemOut, ListCreate, ListOut

app = FastAPI(title="ClaimIt API", version="0.1.0")
init_db()


def user_id_header(x_user_id: Annotated[str | None, Header()] = None) -> str:
    if not x_user_id:
        raise HTTPException(status_code=401, detail="X-User-Id header is required")
    return x_user_id


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/lists", response_model=ListOut, status_code=201)
def create_list(payload: ListCreate, db: Session = Depends(get_db)):
    item_list = ItemList(name=payload.name, code=secrets.token_urlsafe(4).upper())
    db.add(item_list)
    db.commit()
    db.refresh(item_list)
    return item_list


@app.post("/lists/{list_id}/items", response_model=ItemOut, status_code=201)
def create_item(list_id: int, payload: ItemCreate, db: Session = Depends(get_db)):
    if db.get(ItemList, list_id) is None:
        raise HTTPException(status_code=404, detail="list not found")
    item = Item(
        list_id=list_id,
        name=payload.name,
        total=payload.total,
        remaining=payload.total,
    )
    db.add(item)
    db.commit()
    db.refresh(item)
    return item


@app.get("/lists/{list_id}/items", response_model=list[ItemOut])
def list_items(list_id: int, db: Session = Depends(get_db)):
    if db.get(ItemList, list_id) is None:
        raise HTTPException(status_code=404, detail="list not found")
    return (
        db.execute(select(Item).where(Item.list_id == list_id).order_by(Item.id))
        .scalars()
        .all()
    )


@app.get("/items/{item_id}", response_model=ItemOut)
def get_item(item_id: int, db: Session = Depends(get_db)):
    item = db.get(Item, item_id)
    if item is None:
        raise HTTPException(status_code=404, detail="item not found")
    return item


@app.post("/items/{item_id}/claims", response_model=ClaimOut)
def claim_item(
    item_id: int,
    payload: ClaimRequest,
    user_id: str = Depends(user_id_header),
    db: Session = Depends(get_db),
):
    item = db.get(Item, item_id)
    if item is None:
        raise HTTPException(status_code=404, detail="item not found")

    # Atomic check-and-decrement: the UPDATE only succeeds if enough remains.
    statement = (
        update(Item)
        .where(Item.id == item_id, Item.remaining >= payload.quantity)
        .values(remaining=Item.remaining - payload.quantity)
        .returning(Item.remaining)
    )
    remaining = db.execute(statement).scalar_one_or_none()
    if remaining is None:
        db.rollback()
        raise HTTPException(status_code=409, detail="not enough remaining")

    existing = db.execute(
        select(Claim).where(Claim.item_id == item_id, Claim.user_id == user_id)
    ).scalar_one_or_none()
    if existing is None:
        existing = Claim(item_id=item_id, user_id=user_id, quantity=payload.quantity)
        db.add(existing)
    else:
        existing.quantity += payload.quantity

    db.commit()
    return ClaimOut(
        item_id=item_id,
        user_id=user_id,
        quantity=existing.quantity,
        remaining=remaining,
    )


@app.post("/items/{item_id}/releases", response_model=ClaimOut)
def release_item(
    item_id: int,
    payload: ClaimRequest,
    user_id: str = Depends(user_id_header),
    db: Session = Depends(get_db),
):
    item = db.get(Item, item_id)
    if item is None:
        raise HTTPException(status_code=404, detail="item not found")

    existing = db.execute(
        select(Claim).where(Claim.item_id == item_id, Claim.user_id == user_id)
    ).scalar_one_or_none()
    if existing is None or existing.quantity < payload.quantity:
        raise HTTPException(status_code=409, detail="claim does not exist")

    existing.quantity -= payload.quantity
    if existing.quantity == 0:
        db.delete(existing)
    db.execute(
        update(Item)
        .where(Item.id == item_id)
        .values(remaining=Item.remaining + payload.quantity)
    )
    db.commit()

    item = db.get(Item, item_id)
    return ClaimOut(
        item_id=item_id,
        user_id=user_id,
        quantity=existing.quantity if existing.quantity else 0,
        remaining=item.remaining,
    )
