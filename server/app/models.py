from sqlalchemy import ForeignKey, String, UniqueConstraint
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


class ItemList(Base):
    __tablename__ = "lists"

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(120))
    code: Mapped[str] = mapped_column(String(32), unique=True, index=True)


class Item(Base):
    __tablename__ = "items"

    id: Mapped[int] = mapped_column(primary_key=True)
    list_id: Mapped[int] = mapped_column(ForeignKey("lists.id"), index=True)
    name: Mapped[str] = mapped_column(String(120))
    total: Mapped[int]
    remaining: Mapped[int]


class Claim(Base):
    __tablename__ = "claims"

    id: Mapped[int] = mapped_column(primary_key=True)
    item_id: Mapped[int] = mapped_column(ForeignKey("items.id"), index=True)
    user_id: Mapped[str] = mapped_column(String(64), index=True)
    quantity: Mapped[int]

    __table_args__ = (
        UniqueConstraint("item_id", "user_id", name="uq_claim_item_user"),
    )
