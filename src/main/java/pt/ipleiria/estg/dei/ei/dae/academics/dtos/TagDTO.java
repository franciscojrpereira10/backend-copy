package pt.ipleiria.estg.dei.ei.dae.academics.dtos;

import pt.ipleiria.estg.dei.ei.dae.academics.entities.Tag;
import java.util.*;
import java.util.stream.Collectors;

public class TagDTO {

    private Long id;
    private String name;
    private boolean visible;
    private Long createdByUserId;
    private String createdByUsername;
    private Date createdAt;
    private int publicationCount;
    private int subscriberCount;

    public TagDTO() {
    }

    public TagDTO(Long id, String name, boolean visible, Long createdByUserId,
                  String createdByUsername, Date createdAt, int publicationCount,
                  int subscriberCount) {
        this.id = id;
        this.name = name;
        this.visible = visible;
        this.createdByUserId = createdByUserId;
        this.createdByUsername = createdByUsername;
        this.createdAt = createdAt;
        this.publicationCount = publicationCount;
        this.subscriberCount = subscriberCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getPublicationCount() {
        return publicationCount;
    }

    public void setPublicationCount(int publicationCount) {
        this.publicationCount = publicationCount;
    }

    public int getSubscriberCount() {
        return subscriberCount;
    }

    public void setSubscriberCount(int subscriberCount) {
        this.subscriberCount = subscriberCount;
    }

    // Métodos de conversão
    public static TagDTO from(Tag tag) {
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setVisible(tag.isVisible());
        dto.setCreatedAt(tag.getCreatedAt());

        if (tag.getCreatedBy() != null) {
            dto.setCreatedByUserId(tag.getCreatedBy().getId());
            dto.setCreatedByUsername(tag.getCreatedBy().getUsername());
        }

        // Evitar lazy loading das coleções
        dto.setPublicationCount(0);
        dto.setSubscriberCount(0);

        return dto;
    }

    public static List<TagDTO> from(List<Tag> tags) {
        return tags.stream()
                .map(TagDTO::from)
                .collect(Collectors.toList());
    }
}
