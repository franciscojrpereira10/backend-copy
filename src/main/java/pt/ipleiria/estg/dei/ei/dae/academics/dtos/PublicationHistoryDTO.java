package pt.ipleiria.estg.dei.ei.dae.academics.dtos;

import pt.ipleiria.estg.dei.ei.dae.academics.entities.PublicationHistory;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class PublicationHistoryDTO implements Serializable {
    private Long id;
    private Long publicationId;
    private Long userId;
    private String username;
    private Date timestamp;
    private String fieldChanged;
    private String oldValue;
    private String newValue;

    public PublicationHistoryDTO() {
    }

    public PublicationHistoryDTO(Long id, Long publicationId, Long userId, String username,
                                 Date timestamp, String fieldChanged, String oldValue, String newValue) {
        this.id = id;
        this.publicationId = publicationId;
        this.userId = userId;
        this.username = username;
        this.timestamp = timestamp;
        this.fieldChanged = fieldChanged;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Long getId() {
        return id;
    }

    public Long getPublicationId() {
        return publicationId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getFieldChanged() {
        return fieldChanged;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPublicationId(Long publicationId) {
        this.publicationId = publicationId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setFieldChanged(String fieldChanged) {
        this.fieldChanged = fieldChanged;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    // Métodos de conversão
    public static PublicationHistoryDTO from(PublicationHistory history) {
        PublicationHistoryDTO dto = new PublicationHistoryDTO();
        dto.setId(history.getId());

        if (history.getPublication() != null) {
            dto.setPublicationId(history.getPublication().getId());
        }

        if (history.getChangedBy() != null) {
            dto.setUserId(history.getChangedBy().getId());
            dto.setUsername(history.getChangedBy().getUsername());
        }

        dto.setTimestamp(history.getChangedAt());
        dto.setFieldChanged(history.getFieldChanged());
        dto.setOldValue(history.getOldValue());
        dto.setNewValue(history.getNewValue());

        return dto;
    }

    public static List<PublicationHistoryDTO> from(List<PublicationHistory> histories) {
        return histories.stream()
                .map(PublicationHistoryDTO::from)
                .collect(Collectors.toList());
    }
}
