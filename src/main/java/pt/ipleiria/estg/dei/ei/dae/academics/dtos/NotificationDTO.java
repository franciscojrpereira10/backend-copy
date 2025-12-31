package pt.ipleiria.estg.dei.ei.dae.academics.dtos;

import pt.ipleiria.estg.dei.ei.dae.academics.entities.Notification;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationDTO implements Serializable {
    private Long id;
    private String title;
    private String type; // Usar String para o enum no DTO
    private String message;
    private Date createdAt;
    private boolean readFlag;
    private Date readAt;
    private Long userId;
    private String username;
    private Long relatedEntityId;
    private String relatedEntityType;

    public NotificationDTO() {
    }

    public NotificationDTO(Long id, String title, String type, String message, Date createdAt,
                           boolean readFlag, Date readAt, Long userId, String username,
                           Long relatedEntityId, String relatedEntityType) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
        this.readFlag = readFlag;
        this.readAt = readAt;
        this.userId = userId;
        this.username = username;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntityType = relatedEntityType;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public boolean isReadFlag() {
        return readFlag;
    }

    public Date getReadAt() {
        return readAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long getRelatedEntityId() {
        return relatedEntityId;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setReadFlag(boolean readFlag) {
        this.readFlag = readFlag;
    }

    public void setReadAt(Date readAt) {
        this.readAt = readAt;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRelatedEntityId(Long relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public void setRelatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    // Métodos de conversão
    public static NotificationDTO from(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setType(notification.getType() != null ? notification.getType().name() : null);
        dto.setMessage(notification.getMessage());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setReadFlag(notification.isReadFlag());
        dto.setReadAt(notification.getReadAt());

        if (notification.getUser() != null) {
            dto.setUserId(notification.getUser().getId());
            dto.setUsername(notification.getUser().getUsername());
        }

        dto.setRelatedEntityId(notification.getRelatedEntityId());
        dto.setRelatedEntityType(notification.getRelatedEntityType());

        return dto;
    }

    public static List<NotificationDTO> from(List<Notification> notifications) {
        return notifications.stream()
                .map(NotificationDTO::from)
                .collect(Collectors.toList());
    }
}
