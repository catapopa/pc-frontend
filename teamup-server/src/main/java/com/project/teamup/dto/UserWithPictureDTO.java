package com.project.teamup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserWithPictureDTO {
    private UserDTO userToUpdate;
    private String profilePicture;
}