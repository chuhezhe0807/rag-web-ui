package com.chuhezhe.common.constants;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorConstants {

    /**
     * 状态码 503
     */
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "ServiceUnavailable"),

    /**
     * 状态码 500
     */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "InternalError"),

    /**
     * 状态码 400
     */
    REQUEST_TIMEOUT(HttpStatus.BAD_REQUEST, "RequestTimeout"),
    PARAMETER_ERROR(HttpStatus.BAD_REQUEST, "ParameterError"),
    MISSING_REQUEST_HEADER(HttpStatus.BAD_REQUEST, "MissingRequestHeader"),
    USERNAME_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "UsernameAlreadyExist"),
    EMAIL_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "EmailAlreadyExist"),

    /**
     * 状态码 401
     */
    REMOTE_LOGIN(HttpStatus.UNAUTHORIZED, "RemoteLogin"),
    NOT_LOGGED_IN(HttpStatus.UNAUTHORIZED, "NotLoggedIn"),
    ACCOUNT_BANNED(HttpStatus.UNAUTHORIZED, "AccountBanned"),
    LOGGED_EXPIRED(HttpStatus.UNAUTHORIZED, "LoggedExpired"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),
    LOGGING_ERROR(HttpStatus.UNAUTHORIZED, "LoginError"),
    USER_NOT_EXIST(HttpStatus.UNAUTHORIZED, "UserNotExist"),
    USER_DISABLED(HttpStatus.UNAUTHORIZED, "UserDisabled"),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "PayloadTooLarge"),
    UNSUPPORTED_MEDIA(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UnsupportedMedia"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TokenExpired"),

    /**
     * 状态码 403
     */
    NOT_FOUND(HttpStatus.FORBIDDEN, "NotFound"),
    SIGNATURE_DOES_NOT_MATCH(HttpStatus.FORBIDDEN, "SignatureDoesNotMatch"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AccessDenied"),
    NO_ACCESS_PERMISSION(HttpStatus.FORBIDDEN, "NoAccessPermission"),
    ILLEGAL_RESOURCE_VISIT(HttpStatus.FORBIDDEN, "IllegalResourceVisit"),
    UPLOAD_NOT_ALLOWED(HttpStatus.FORBIDDEN, "UploadNotAllowed"),

    /**
     * 状态码 404
     */
    PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "PermissionNotFound"),
    RESOURCE_NOT_EXIST(HttpStatus.NOT_FOUND, "ResourceNotExist"),
    USER_PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "UserPermissionNotFound"),

    /**
     * 文件校验相关错误
     */
    FILE_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "FileValidationError"),
    INVALID_EXECUTABLE_FORMAT(HttpStatus.BAD_REQUEST, "InvalidExecutableFormat"),

    NOT_FOUND_API(HttpStatus.NOT_FOUND, "NotFoundApi"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "MethodNotAllowed");

    /**
     * http状态码
     */
    private final HttpStatus httpStatus;

    /**
     * 错误码
     */
    private final String code;

    ErrorConstants(HttpStatus httpStatus, String code) {
        this.httpStatus = httpStatus;
        this.code = code;
    }
}
