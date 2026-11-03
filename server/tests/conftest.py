import os

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.db import get_db
from app.main import app
from app.models import Base

TEST_DATABASE_URL = os.getenv("TEST_DATABASE_URL", "sqlite:///./test_claimit.db")
CONNECT_ARGS = {"check_same_thread": False} if TEST_DATABASE_URL.startswith("sqlite") else {}
engine = create_engine(TEST_DATABASE_URL, connect_args=CONNECT_ARGS, future=True)
TestingSessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False, future=True)


@pytest.fixture(autouse=True)
def reset_db():
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)
    yield


@pytest.fixture
def client():
    def override_get_db():
        db = TestingSessionLocal()
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()
