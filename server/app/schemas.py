from pydantic import BaseModel, ConfigDict, Field, field_validator


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


def _normalise_email(value: str) -> str:
    value = value.strip().lower()
    if "@" not in value:
        raise ValueError("invalid email")
    return value


class RegisterRequest(BaseModel):
    email: str = Field(min_length=3, max_length=254)
    password: str = Field(min_length=8, max_length=128)
    display_name: str = Field(min_length=1, max_length=80)

    @field_validator("email")
    @classmethod
    def validate_email(cls, value: str) -> str:
        return _normalise_email(value)


class LoginRequest(BaseModel):
    email: str = Field(min_length=3, max_length=254)
    password: str = Field(min_length=1, max_length=128)

    @field_validator("email")
    @classmethod
    def validate_email(cls, value: str) -> str:
        return _normalise_email(value)


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


class UserOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    email: str
    display_name: str
