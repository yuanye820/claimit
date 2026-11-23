import secrets

from fastapi import Depends, FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import select, update
from sqlalchemy.orm import Session

from .auth import create_access_token, current_user, hash_password, verify_password
from .db import get_db, init_db
from .models import Claim, Item, ItemList, User
from .schemas import (
    ClaimOut,
    ClaimRequest,
    ItemCreate,
    ItemOut,
    ListCreate,
    ListOut,
    LoginRequest,
    RegisterRequest,
    TokenResponse,
    UserOut,
)

app = FastAPI(title="ClaimIt API", version="0.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://127.0.0.1:8080"],
    allow_methods=["*"],
    allow_headers=["*"],
)
init_db()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/auth/register", response_model=TokenResponse, status_code=201)
def register(payload: RegisterRequest, db: Session = Depends(get_db)):
    existing = db.execute(
        select(User).where(User.email == payload.email)
    ).scalar_one_or_none()
    if existing is not None:
        raise HTTPException(status_code=409, detail="email already registered")

    user = User(
        email=payload.email,
        password_hash=hash_password(payload.password),
        display_name=payload.display_name,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return TokenResponse(access_token=create_access_token(user.id))


@app.post("/auth/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    user = db.execute(
        select(User).where(User.email == payload.email)
    ).scalar_one_or_none()
    if user is None or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=401, detail="invalid credentials")
    return TokenResponse(access_token=create_access_token(user.id))


@app.get("/auth/me", response_model=UserOut)
def me(user: User = Depends(current_user)):
    return user


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
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
):
    user_id = str(user.id)
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
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
):
    user_id = str(user.id)
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
