from pydantic import BaseModel, ConfigDict, Field


class ListCreate(BaseModel):
    name: str = Field(min_length=1, max_length=120)


class ListOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    code: str


class ItemCreate(BaseModel):
    name: str = Field(min_length=1, max_length=120)
    total: int = Field(gt=0)


class ItemOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    list_id: int
    name: str
    total: int
    remaining: int


class ClaimRequest(BaseModel):
    quantity: int = Field(gt=0)


class ClaimOut(BaseModel):
    item_id: int
    user_id: str
    quantity: int
    remaining: int
