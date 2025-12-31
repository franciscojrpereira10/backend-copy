package pt.ipleiria.estg.dei.ei.dae.academics.dtos;

import pt.ipleiria.estg.dei.ei.dae.academics.entities.Activity;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ActivityDTO implements Serializable {
    private Long id;
    private String type; // ActivityType em String
    private String description;
    private String username;
    private Long userId;
    private String entityType;
    private Long entityId;
    private Date timestamp;

    public ActivityDTO() {
    }

    public ActivityDTO(Long id, String type, String description, String username,
                       Long userId, String entityType, Long entityId, Date timestamp) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.username = username;
        this.userId = userId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getUsername() {
        return username;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    // Métodos de conversão
    public static ActivityDTO from(Activity activity) {
        ActivityDTO dto = new ActivityDTO();
        dto.setId(activity.getId());
        dto.setType(activity.getType() != null ? activity.getType().name() : null);
        dto.setDescription(activity.getDescription());

        if (activity.getUser() != null) {
            dto.setUserId(activity.getUser().getId());
            dto.setUsername(activity.getUser().getUsername());
        }

        dto.setEntityType(activity.getEntityType());
        dto.setEntityId(activity.getEntityId());
        dto.setTimestamp(activity.getTimestamp());

        return dto;
    }

    public static List<ActivityDTO> from(List<Activity> activities) {
        return activities.stream()
                .map(ActivityDTO::from)
                .collect(Collectors.toList());
    }
}
