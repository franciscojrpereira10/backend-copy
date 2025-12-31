package pt.ipleiria.estg.dei.ei.dae.academics.dtos;

import pt.ipleiria.estg.dei.ei.dae.academics.entities.CommentEditHistory;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CommentEditHistoryDTO implements Serializable {
    private Long id;
    private Long commentId;
    private String oldContent;
    private String newContent;
    private Date editedAt;
    private Long userId;
    private String username;

    public CommentEditHistoryDTO() {
    }

    public CommentEditHistoryDTO(Long id, Long commentId, String oldContent, String newContent,
                                 Date editedAt, Long userId, String username) {
        this.id = id;
        this.commentId = commentId;
        this.oldContent = oldContent;
        this.newContent = newContent;
        this.editedAt = editedAt;
        this.userId = userId;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public Long getCommentId() {
        return commentId;
    }

    public String getOldContent() {
        return oldContent;
    }

    public String getNewContent() {
        return newContent;
    }

    public Date getEditedAt() {
        return editedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public void setOldContent(String oldContent) {
        this.oldContent = oldContent;
    }

    public void setNewContent(String newContent) {
        this.newContent = newContent;
    }

    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // Métodos de conversão
    public static CommentEditHistoryDTO from(CommentEditHistory history) {
        CommentEditHistoryDTO dto = new CommentEditHistoryDTO();
        dto.setId(history.getId());

        if (history.getComment() != null) {
            dto.setCommentId(history.getComment().getId());
        }

        dto.setOldContent(history.getOldContent());
        dto.setNewContent(history.getNewContent());
        dto.setEditedAt(history.getEditedAt());

        if (history.getEditedBy() != null) {
            dto.setUserId(history.getEditedBy().getId());
            dto.setUsername(history.getEditedBy().getUsername());
        }

        return dto;
    }

    public static List<CommentEditHistoryDTO> from(List<CommentEditHistory> histories) {
        return histories.stream()
                .map(CommentEditHistoryDTO::from)
                .collect(Collectors.toList());
    }
}
