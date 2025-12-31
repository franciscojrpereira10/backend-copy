package pt.ipleiria.estg.dei.ei.dae.academics.dtos;

import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.Role;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.UserStatus;

import java.util.ArrayList;
import java.util.List;

public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private UserStatus status;
    private String createdAt;
    private String lastLogin;

    private int publicationCount;
    private int subscribedTagsCount;
    private int commentsCount;
    private int ratingsCount;
    private List<String> subscribedTags;

    public UserDTO() {
        this.subscribedTags = new ArrayList<>();
    }

    public UserDTO(Long id, String username, String email, Role role, UserStatus status,
                   String createdAt, String lastLogin, int publicationCount, int subscribedTagsCount,
                   int commentsCount, int ratingsCount, List<String> subscribedTags) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.publicationCount = publicationCount;
        this.subscribedTagsCount = subscribedTagsCount;
        this.commentsCount = commentsCount;
        this.ratingsCount = ratingsCount;
        this.subscribedTags = subscribedTags != null ? subscribedTags : new ArrayList<>();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }

    public int getPublicationCount() { return publicationCount; }
    public void setPublicationCount(int publicationCount) { this.publicationCount = publicationCount; }

    public int getSubscribedTagsCount() { return subscribedTagsCount; }
    public void setSubscribedTagsCount(int subscribedTagsCount) { this.subscribedTagsCount = subscribedTagsCount; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

    public int getRatingsCount() { return ratingsCount; }
    public void setRatingsCount(int ratingsCount) { this.ratingsCount = ratingsCount; }

    public List<String> getSubscribedTags() { return subscribedTags; }
    public void setSubscribedTags(List<String> subscribedTags) {
        this.subscribedTags = subscribedTags != null ? subscribedTags : new ArrayList<>();
    }

    public static UserDTO from(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toInstant().toString() : null);
        dto.setLastLogin(user.getLastLogin() != null ? user.getLastLogin().toInstant().toString() : null);

        dto.setPublicationCount(0);
        dto.setSubscribedTagsCount(0);
        dto.setCommentsCount(0);
        dto.setRatingsCount(0);
        dto.setSubscribedTags(new ArrayList<>());

        return dto;
    }

    public static List<UserDTO> from(List<User> users) {
        return users.stream()
                .map(UserDTO::from)
                .toList();
    }

    public static UserDTO fromLogin(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toInstant().toString() : null);
        return dto;
    }
}
