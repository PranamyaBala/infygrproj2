package com.hostel.booking.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
}
