import sqlite3
from contextlib import contextmanager
from unittest.mock import Mock
import pytest
from repository import DatabaseConnection, UserRepository


@pytest.fixture
def database():
    conn = sqlite3.connect(':memory:')
    # TODO: init database with tables/values

    yield conn
    conn.close()

@pytest.fixture
def db_connection():
    @contextmanager
    def get_connection():
        yield database
    db_connection =  Mock(spec=DatabaseConnection)

    db_connection.get_connection = get_connection
    return db_connection

@pytest.fixture
def user_repository(db_connection):
    return UserRepository(db_connection)