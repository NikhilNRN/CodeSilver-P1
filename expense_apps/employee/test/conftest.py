import sqlite3
from contextlib import contextmanager
from unittest.mock import Mock
from repository import DatabaseConnection, UserRepository
import pytest
from flask import Flask
from api.auth_controller import auth_bp  # <- blueprint is here

@pytest.fixture
def app():
    app = Flask(__name__)
    app.config["TESTING"] = True
    app.secret_key = "testsecret"

    app.register_blueprint(auth_bp, url_prefix="/api/auth")
    yield app

@pytest.fixture
def client(app):
    return app.test_client()

@pytest.fixture(autouse=True)
def patch_auth_service(monkeypatch):
    from unittest.mock import MagicMock
    import api.auth_controller as auth_module  # <- correct module

    mock_auth_service = MagicMock()
    monkeypatch.setattr(auth_module, "get_auth_service", lambda: mock_auth_service)
    return mock_auth_service


@pytest.fixture
def database():
    conn = sqlite3.connect(':memory:')
    # TODO: init database with tables/values
    sqlite_script = """
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT UNIQUE NOT NULL,
            password TEXT NOT NULL,
            role TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS expenses (
            id INTEGER PRIMARY KEY AUTOINCREMENT, 
            user_id INTEGER NOT NULL, 
            amount REAL NOT NULL,  
            description TEXT NOT NULL,
            date TEXT NOT NULL,
            FOREIGN KEY (user_id) REFERENCES users (id)
        );
        CREATE TABLE IF NOT EXISTS approvals (
            id INTEGER PRIMARY KEY AUTOINCREMENT, 
            expense_id INTEGER NOT NULL,
            status TEXT NOT NULL,
            reviewer INTEGER,
            comment TEXT,
            review_date TEXT,
            FOREIGN KEY (expense_id) REFERENCES expenses (id),
            FOREIGN KEY (reviewer) REFERENCES users (id)
        );
    """
    sample_users = [
        ("employee1", "password123", "Employee"),
        ("manager1", "password123", "Manager"),
        ("employee2", "password123", "Employee"),
        ("employee3", "password123", "Employee")
    ]
    sample_expenses = [
        (1, 22.22, "Food", "2025-12-01"),
        (1, 39.55, "Gas", "2025-12-01"),
        (1, 0.01, "Min Boundary Test Expense", "2025-12-01"),
        (1, 1000000000000000000000000, "Test Expense 2 loooooooooooooooooooooooooooooooooooooooool", "2026-01-01"),
        (1, 82.57, "Test Expense 3", "2026-01-01"),
        (1, 2.49, "Business-related handkerchief", "2025-12-10"),
        (3, 5.19, "Pizzer (It's hot and it's ready)", "2025-12-10"),
        (3, 800.97, "Togore-tastic Training", "2025-12-10"),
        (3, 70, "Super Mario Party for the Nintendo Switch", "2025-12-10"),
        (4, 75, "software", "2025-11-20"),
        (4, 1.43, "Party Time", "2025-11-20"),
        (4, 43.55, "Gas", "2025-12-10")
    ]
    sample_approvals = [
        (1, "pending", None, None, None),
        (2, "pending", None, None, None),
        (3, "pending", None, None, None),
        (4, "pending", None, None, None),
        (5, "pending", None, None, None),
        (6, "pending", None, None, None),
        (7, "pending", None, None, None),
        (8, "pending", None, None, None),
        (9, "pending", None, None, None),
        (10, "pending", None, None, None),
        (11, "pending", None, None, None),
        (12, "pending", None, None, None)
    ]
    query_insert_users = "INSERT INTO users (username, password, role) VALUES (?, ?, ?);"
    query_insert_expenses = "INSERT INTO expenses (user_id, amount, description, date) VALUES (?, ?, ?, ?);"
    query_insert_approvals = "INSERT INTO approvals (expense_id, status, reviewer, comment, review_date) VALUES (?, ?, ?, ?, ?);"

    with conn:
        cursor = conn.cursor()
        cursor.executescript(sqlite_script)
        conn.commit()
        cursor.executemany(query_insert_users, sample_users)
        conn.commit()
        cursor.executemany(query_insert_expenses, sample_expenses)
        conn.commit()
        cursor.executemany(query_insert_approvals, sample_approvals)
        conn.commit()

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