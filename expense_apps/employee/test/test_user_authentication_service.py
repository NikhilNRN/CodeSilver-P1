from unittest import mock

import jwt
import pytest
from unittest.mock import Mock, MagicMock
from datetime import datetime, timedelta
from pytest_mock import mocker

from repository import User
from service import ExpenseService, AuthenticationService


@pytest.fixture
def mock_user_repository():
    return Mock()

@pytest.fixture()
def jwt_key_test():
    return "test_key"


@pytest.fixture
def authentication_service(mock_user_repository, jwt_key_test):
    return AuthenticationService(
        user_repository=mock_user_repository,
        jwt_secret_key=jwt_key_test
    )
#authenticate_user

#test valid user pass
def test_authenticate_valid(authentication_service, mock_user_repository):
    uname = "John"
    pword = "Pass"
    exp_user = Mock()
    exp_user.password = pword
    exp_user.username = uname

    mock_user_repository.find_by_username.return_value = exp_user
    ret_user = authentication_service.authenticate_user(uname, pword)

    assert ret_user.username == uname

#test invlid user (no repo response)
#test invalid pass (authenticate return none)
@pytest.mark.parametrize("name, password",
                         [
                             ("John", "pass"),
                             ("John", "PASS"),
                             (None, "Pass"),
                             (None, None),
                             ("John", None)
                         ])
def test_authenticate_invalid(authentication_service, mock_user_repository, name, password):
    exp_user = Mock()
    exp_user.password = password
    exp_user.username = name

    if name != None:
        mock_user_repository.find_by_username.return_value = exp_user
    else:
        mock_user_repository.find_by_username.return_value = None
    ret_user = authentication_service.authenticate_user(name, "Pass")

    assert not ret_user

#this function only makes one call, so we simply verify that it is doing as such
def test_get_id_valid(authentication_service, mock_user_repository):
    uname = "John"
    pword = "Pass"
    exp_user = Mock()
    exp_user.password = pword
    exp_user.username = uname

    mock_user_repository.find_by_id.return_value = exp_user

    ret_user = authentication_service.get_user_by_id(1)
    assert ret_user.username==uname

def test_get_id_invalid(authentication_service, mock_user_repository):

    mock_user_repository.find_by_id.return_value = None

    ret_user = authentication_service.get_user_by_id(1)
    assert None == ret_user


def test_gentoken_valid(authentication_service):
    uname = "John"
    pword = "Pass"
    exp_user = Mock()
    exp_user.password = pword
    exp_user.username = uname
    exp_user.role = "Employee"
    exp_user.id = 1
    ret_encode= authentication_service.generate_jwt_token(exp_user)
    # print(ret_encode)
    userdat = jwt.decode(ret_encode,authentication_service.jwt_secret_key, authentication_service.jwt_algorithm)
    assert userdat['username'] == uname
    assert userdat['user_id'] == 1

@pytest.mark.parametrize("name, password, role, id",
                         [
                             ("John", None, "Employee", 1),
                             ("John", "pass", None, 1),
                             ("John", "pass", "Employee", None),
                             (None, None, None, None)
                         ])
def test_gentoken_params(authentication_service, name, password, role, id):
    exp_user = Mock()
    exp_user.password = password
    exp_user.username = name
    exp_user.role = role
    exp_user.id = id
    ret_encode = authentication_service.generate_jwt_token(exp_user)
    # print(ret_encode)
    userdat = jwt.decode(ret_encode, authentication_service.jwt_secret_key, authentication_service.jwt_algorithm)
    print (userdat)
    assert userdat['username'] == name
    assert userdat['role'] == role
    assert userdat['user_id'] == id


def test_validate_token_old(authentication_service):
    payload = {
        'user_id': 1,
        'username': "John",
        'role': "Employee",
        'exp': datetime(1999, 1, 1) + timedelta(24),
        'iat': datetime.utcnow()
    }
    egg =  jwt.encode(payload, authentication_service.jwt_secret_key, algorithm=authentication_service.jwt_algorithm)
    result = authentication_service.validate_jwt_token(egg)
    #should fail due to timeout
    assert not result

def test_validate_token_invalid(authentication_service):
    payload = {
        'user_id': 1,
        'username': "John",
        'role': "Employee",
        'exp': datetime.utcnow() + timedelta(24),
        'iat': datetime.utcnow()
    }
    egg = None
    result = authentication_service.validate_jwt_token(egg)
    #should fail due to timeout
    assert not result


#token contains a valid user id in its payload
def test_getbytoken_valid(authentication_service, mock_user_repository):
    payload = MagicMock()
    d = {'user_id': 1}
    payload.__getitem__.side_effect = d.__getitem__
    user = Mock()
    with mock.patch.object(authentication_service, "validate_jwt_token") as mock_validate:
        mock_validate.return_value = payload
        mock_user_repository.find_by_id.return_value = user

        ret_user = authentication_service.get_user_from_token("a")
        assert ret_user

#user id inside is invalid
def test_getbytoken_invalid_id(authentication_service, mock_user_repository):
    payload = MagicMock()
    d = {'user_id': None}
    payload.__getitem__.side_effect = d.__getitem__
    with mock.patch.object(authentication_service, "validate_jwt_token") as mock_validate:
        mock_validate.return_value = payload
        mock_user_repository.find_by_id.return_value = None

        ret_user = authentication_service.get_user_from_token("a")
        assert not ret_user


#token validation fails
def test_getbytoken_invalid_token(authentication_service, mock_user_repository):
    payload = MagicMock()
    d = {'user_id': None}
    payload.__getitem__.side_effect = d.__getitem__
    with mock.patch.object(authentication_service, "validate_jwt_token") as mock_validate:
        mock_validate.return_value = None
        ret_user = authentication_service.get_user_from_token("a")
        assert not ret_user