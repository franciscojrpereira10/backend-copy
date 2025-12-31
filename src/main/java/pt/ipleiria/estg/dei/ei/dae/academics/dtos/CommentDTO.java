package pt.ipleiria.estg.dei.ei.dae.academics.dtos;

import pt.ipleiria.estg.dei.ei.dae.academics.entities.Comment;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CommentDTO implements Serializable {
    private Long id;
    private String content;
    private String username;
    private Long userId;
    private Long publicationId;
    private Date createdAt;
    private boolean visible;
    private String visibleReason;
    private Date editedAt;
    private Date updatedAt;

    public CommentDTO() {
    }

    public CommentDTO(Long id, String content, String username, Long userId,
                      Long publicationId, Date createdAt, boolean visible, String visibleReason,
                      Date editedAt, Date updatedAt) {
        this.id = id;
        this.content = content;
        this.username = username;
        this.userId = userId;
        this.publicationId = publicationId;
        this.createdAt = createdAt;
        this.visible = visible;
        this.visibleReason = visibleReason;
        this.editedAt = editedAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getUsername() {
        return username;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getPublicationId() {
        return publicationId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getVisibleReason() {
        return visibleReason;
    }

    public Date getEditedAt() {
        return editedAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setPublicationId(Long publicationId) {
        this.publicationId = publicationId;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setVisibleReason(String visibleReason) {
        this.visibleReason = visibleReason;
    }

    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Métodos de conversão
    public static CommentDTO from(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());

        if (comment.getAuthor() != null) {
            dto.setUserId(comment.getAuthor().getId());
            dto.setUsername(comment.getAuthor().getUsername());
        }

        if (comment.getPublication() != null) {
            dto.setPublicationId(comment.getPublication().getId());
        }

        dto.setCreatedAt(comment.getCreatedAt());
        dto.setVisible(comment.isVisible());
        dto.setVisibleReason(comment.getVisibleReason());
        dto.setEditedAt(comment.getEditedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());

        return dto;
    }

    public static List<CommentDTO> from(List<Comment> comments) {
        return comments.stream()
                .map(CommentDTO::from)
                .collect(Collectors.toList());
    }
}
