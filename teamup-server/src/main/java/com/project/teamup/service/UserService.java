package com.project.teamup.service;

import com.project.teamup.dao.UserRepository;
import com.project.teamup.dao.VerificationTokenRepository;
import com.project.teamup.model.FilterCriterias;
import com.project.teamup.model.User;
import com.project.teamup.model.UserRole;
import com.project.teamup.model.VerificationToken;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final static int VERIFICATION_TOKEN_VALIDITY = 60 * 60 * 1000;
    private final static String WEB_APP_URL = "http://localhost:4200/registration/";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VerificationTokenRepository tokenRepository;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private MailService mailService;

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User save(@Valid User user) {
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User createProfile(User user, Long id, Blob blob) {
        Optional<User> optionalUser = userRepository.findById(id);
        User userToUpdate = new User();
        if (optionalUser.isPresent()) {
            userToUpdate = optionalUser.get();
        }

        if (user.getPicture() != null) {
            try {
                byte[] bytes;
                bytes = blob.getBytes(1L, (int) blob.length());
                userToUpdate.setPicture(bytes);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (user.getLanguage() != null) {
            userToUpdate.setLanguage(user.getLanguage());
        }
        if (user.getSeniority() != null) {
            userToUpdate.setSeniority(user.getSeniority());
        }
        if (user.getLocation() != null) {
            userToUpdate.setLocation(user.getLocation());
        }
        if (user.getCompany() != null) {
            userToUpdate.setCompany(user.getCompany());
        }
        if (user.getSkills().size() != 0) {
            userToUpdate.setSkills(user.getSkills());
        }
        if (user.getProjectExperiences().size() != 0) {
            userToUpdate.setProjectExperiences(user.getProjectExperiences());
        }

        return userRepository.save(userToUpdate);
    }

    public List<User> filterUser(Map<FilterCriterias, String> criteria) {
        List<User> userList = new ArrayList<>();
        for (Map.Entry<FilterCriterias, String> entry : criteria.entrySet()) {
            switch (entry.getKey()) {
                case ROLE:
                    userList.addAll(getAll().stream()
                            .filter(user -> user.getRole() != null)
                            .filter(user -> String.valueOf(user.getRole())
                                    .equalsIgnoreCase(String.valueOf(entry.getValue())))
                            .collect(Collectors.toList()));
                    break;
                case SENIORITY:
                    userList.addAll(getAll().stream()
                            .filter(user -> user.getSeniority() != null)
                            .filter(user -> String.valueOf(user.getSeniority())
                                    .equalsIgnoreCase(String.valueOf(entry.getValue()))).collect(Collectors.toList()));
                    break;
                case TECHNOLOGY: {
                    List<User> users = getAll();
                    users.removeIf(user -> user.getSkills()
                            .stream()
                            .anyMatch(skill -> String.valueOf(skill.getTechnology().getName())
                                    .equalsIgnoreCase(String.valueOf(entry.getValue()))));
                    userList.addAll(users);
                    break;
                }
                case SKILL_LEVEL: {
                    List<User> users = getAll();
                    users.removeIf(user -> user.getSkills()
                            .stream()
                            .anyMatch(skill -> String.valueOf(skill.getLevel())
                                    .equalsIgnoreCase(String.valueOf(entry.getValue()))));
                    userList.addAll(users);
                    break;
                }
                case LOCATION:
                    userList.addAll(getAll().stream()
                            .filter(user -> user.getLocation() != null)
                            .filter(user -> String.valueOf(user.getLocation().getCity())
                                    .equalsIgnoreCase(String.valueOf(entry.getValue()))).collect(Collectors.toList()));
                    break;
                case LANGUAGE:
                    userList.addAll(getAll().stream()
                            .filter(user -> user.getLanguage() != null)
                            .filter(user -> String.valueOf(user.getLanguage())
                                    .equalsIgnoreCase(String.valueOf(entry.getValue()))).collect(Collectors.toList()));
                    break;
            }
        }
        return userList.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Activates a blocked user by setting him
     * the number of login attempts to 0 and a new generated
     * password which will be sent by email.
     *
     * @param adminUsername username of the admin who activates the blocked user
     * @param emplUsername  username of the user that becomes a new password
     *                      via email from the admin
     */
    public String activateUser(String adminUsername, String emplUsername) {
        Optional<User> deactivatedUser = userRepository.findByUsername(emplUsername);
        Optional<User> admin = userRepository.findByUsername(adminUsername);
        if (deactivatedUser.isPresent() && admin.isPresent()) {
            User userToBeActivated = deactivatedUser.get();
            if (userToBeActivated.getFailedLoginAttempts() >= 3) {
                String newPassword = generateNewPassword();
                mailService.sendEmailActivation(admin.get(), userToBeActivated, newPassword);
                userToBeActivated.setFailedLoginAttempts(0); //activates the user
                userToBeActivated.setPassword(bCryptPasswordEncoder.encode(newPassword));
                userRepository.save(userToBeActivated);
                return "User was successfully activated and a mail with the new password was sent to him!";
            } else {
                return "The user is already activated!";
            }
        } else {
            return "Invalid username of admin/employee!";
        }
    }

    public String generateActivationLink(User user) {
        VerificationToken verificationToken = tokenRepository.findByUser(user);
        return WEB_APP_URL + verificationToken.getToken();
    }

    public VerificationToken createVerificationTokenForUser(User user) {
        VerificationToken verificationToken = tokenRepository.findByUser(user);
        if (verificationToken == null) {
            return tokenRepository.save(new VerificationToken(null, UUID.randomUUID().toString(), user, new Timestamp(new Date().getTime() + VERIFICATION_TOKEN_VALIDITY)));
        } else {
            if (verificationToken.getExpiryDate().after(new Timestamp(new Date().getTime()))) {
                tokenRepository.delete(verificationToken);
                return tokenRepository.save(new VerificationToken(null, UUID.randomUUID().toString(), user, new Timestamp(new Date().getTime() + VERIFICATION_TOKEN_VALIDITY)));
            }
        }
        return null;
    }

    /**
     * Generates a random string of length 6
     * which represents the new password for
     * a user who must be activated.
     */
    String generateNewPassword() {
        int length = 6;
        boolean useLetters = true;
        boolean useNumbers = true;
        return RandomStringUtils.random(length, useLetters, useNumbers);
    }

    public boolean isValidToken(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token);
        if (verificationToken != null && verificationToken.getExpiryDate().before(new Timestamp(new Date().getTime()))) {
            tokenRepository.delete(verificationToken);
            return true;
        }
        return false;
    }

    /**
     * Returns a list of users, to which the user with the corresponding ID is responsible of.
     *
     * @param id {@link Long}, to which the entity is identified by and represents a user.
     * @return an {@link List<User>} of the corresponding User, if the {@link User} with the id exists and it is a {SUPERVISOR},
     * an {@link Optional} that is empty, if the user does not exist/is not a supervisor or is responsible of no one.
     * @author Sebastian
     */
    public Optional<List<User>> getAssignedUsers(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            if (user.get().getRole().equals(UserRole.SUPERVISOR)) {
                return userRepository.findUsersBySupervisorId(id);
            }
        } else {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
