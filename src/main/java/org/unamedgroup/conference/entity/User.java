package org.unamedgroup.conference.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * User
 *
 * @author liumengxiao
 * @date 2019/03/12
 */


@Entity
@Data
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Integer userID;
    private String password;
    private String realName;
    private String department;
    private String email;
    private String phoneNumber;
    private Integer userGroup;
    @Lob
    private String feature;

    @Transient
    public final static HashMap<Integer, String> USERGROUP = new HashMap<Integer, String>() {{
        put(1, "普通用户");
        put(2, "经理");
        put(3, "后勤");
        put(0, "系统管理员");
    }};

    public User() {
    }

    public User(String password, String realName, String department, String email, String phoneNumber, Integer userGroup, String feature) {
        if (getPasswordHash(password) == null) {
            throw new NullPointerException();
        }
        this.password = getPasswordHash(password);
        this.realName = realName;
        this.department = department;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.userGroup = userGroup;
        this.feature = feature;
    }

    public static String getPasswordHash(String password) {
        try {
            if (password != null) {
                Integer passwordHash = password.hashCode();
                return passwordHash.toString();
            } else {
                throw new NullPointerException();
            }
        } catch (Exception e) {
            System.err.println("密码进行HASH运算时候遇到问题，请检查。");
            System.err.println(e.toString());
        }
        return null;
    }

}
