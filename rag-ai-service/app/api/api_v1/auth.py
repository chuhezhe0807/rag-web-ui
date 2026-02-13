from datetime import timedelta
from typing import Any
from fastapi import APIRouter, Depends, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from jose import JWTError, jwt
from requests.exceptions import RequestException

from app.core import security
from app.core.config import settings
from app.db.session import get_db
from app.models.user import User
from app.schemas.base import ApiResponse
from app.schemas.token import Token
from app.schemas.user import UserCreate, UserResponse

router = APIRouter()
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")

def get_current_user(
    db: Session = Depends(get_db),
    token: str = Depends(oauth2_scheme)
) -> User:
    credentials_exception = ApiResponse(
        code=status.HTTP_401_UNAUTHORIZED,
        codeDesc="Unauthorized",
        message="Could not validate credentials"
    )

    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        username: str = payload.get("username")
        if username is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception
    
    user = db.query(User).filter(User.username == username).first()
    if user is None:
        raise credentials_exception
    return user

@router.post("/register", response_model=ApiResponse[UserResponse])
def register(*, db: Session = Depends(get_db), user_in: UserCreate) -> ApiResponse[UserResponse]:
    """
    Register a new user.
    """
    try:
        # Check if user with this email exists
        user = db.query(User).filter(User.email == user_in.email).first()
        if user:
            return ApiResponse(
                code=status.HTTP_400_BAD_REQUEST,
                codeDesc="Bad Request",
                message="A user with this email already exists."
            )
        
        # Check if user with this username exists
        user = db.query(User).filter(User.username == user_in.username).first()
        if user:
            return ApiResponse(
                code=status.HTTP_400_BAD_REQUEST,
                codeDesc="Bad Request",
                message="A user with this username already exists."
            )
        
        # Create new user
        user = User(
            email=user_in.email,
            username=user_in.username,
            hashed_password=security.get_password_hash(user_in.password),
        )
        db.add(user)
        db.commit()
        db.refresh(user)
        return ApiResponse(
            code=status.HTTP_201_CREATED,
            codeDesc="Created",
            message="User created successfully",
            data=user
        )
    except RequestException as e:
        return ApiResponse(
            code=status.HTTP_503_SERVICE_UNAVAILABLE,
            codeDesc="Service Unavailable",
            message="Network error or server is unreachable. Please try again later.",
        )

@router.post("/token", response_model=ApiResponse[Token])
def login_access_token(
    db: Session = Depends(get_db), form_data: OAuth2PasswordRequestForm = Depends()
) -> ApiResponse[Token]:
    """
    OAuth2 compatible token login, get an access token for future requests.
    """
    user = db.query(User).filter(User.username == form_data.username).first()
    if not user or not security.verify_password(form_data.password, user.hashed_password):
        return ApiResponse(
            code=status.HTTP_401_UNAUTHORIZED,
            codeDesc="Unauthorized",
            message="Incorrect username or password"
        )
    elif not user.is_active:
        return ApiResponse(
            code=status.HTTP_401_UNAUTHORIZED,
            codeDesc="Unauthorized",
            message="Inactive user"
        )
    
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = security.create_access_token(
        data={"username": user.username}, expires_delta=access_token_expires
    )
    return ApiResponse(
        code=status.HTTP_200_OK,
        codeDesc="OK",
        message="Login successful",
        data=Token(access_token = access_token, token_type = "bearer")
    )

@router.post("/test-token", response_model=UserResponse)
def test_token(current_user: User = Depends(get_current_user)) -> Any:
    """
    Test access token by getting current user.
    """
    return current_user


if __name__ == "__main__":
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = security.create_access_token(
        data={"username": "chu"}, expires_delta=access_token_expires
    )
    print(access_token_expires)
    print(access_token)
