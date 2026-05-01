package com.example.library.common.vo;

import java.util.Objects;

/**
 * 서비스 간 메시지와 도메인 모델에서 공통으로 사용하는 사용자 식별 값 객체입니다.
 */
public class IDName {
    private String id;
    private String name;

    /**
     * JSON 역직렬화와 프레임워크 바인딩을 위한 기본 생성자입니다.
     */
    public IDName() {
    }

    /**
     * 사용자 ID와 이름을 함께 보관하는 식별 값을 생성합니다.
     *
     * @param id 회원을 식별하는 문자열 ID입니다.
     * @param name 회원 이름입니다.
     */
    public IDName(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * 사용자의 시스템 식별자를 반환합니다.
     *
     * @return 회원 ID를 반환합니다.
     */
    public String getId() {
        return id;
    }

    /**
     * 메시지 역직렬화 시 사용자의 시스템 식별자를 설정합니다.
     *
     * @param id 회원을 식별하는 문자열 ID입니다.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 사용자의 표시 이름을 반환합니다.
     *
     * @return 회원 이름을 반환합니다.
     */
    public String getName() {
        return name;
    }

    /**
     * 메시지 역직렬화 시 사용자의 표시 이름을 설정합니다.
     *
     * @param name 회원 이름입니다.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 같은 사용자 식별 값인지 ID와 이름 기준으로 비교합니다.
     *
     * @param o 동등성 비교 대상 객체입니다.
     * @return 회원 ID와 이름이 모두 같으면 true, 하나라도 다르면 false를 반환합니다.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IDName idName)) {
            return false;
        }
        return Objects.equals(id, idName.id) && Objects.equals(name, idName.name);
    }

    /**
     * ID와 이름을 기준으로 값 객체 해시를 계산합니다.
     *
     * @return 계산된 정수 값을 반환합니다.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
