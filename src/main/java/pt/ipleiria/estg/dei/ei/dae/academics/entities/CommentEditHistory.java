package pt.ipleiria.estg.dei.ei.dae.academics.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "comment_edit_history")
public class CommentEditHistory implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Column(length = 2000, nullable = false)
    private String oldContent;

    @Column(length = 2000)
    private String newContent;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "edited_at", nullable = false)
    private Date editedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by_id")
    private User editedBy;

    public CommentEditHistory() {}

    public CommentEditHistory(Long id, Comment comment, String oldContent, String newContent, Date editedAt, User editedBy) {
        this.id = id;
        this.comment = comment;
        this.oldContent = oldContent;
        this.newContent = newContent;
        this.editedAt = editedAt;
        this.editedBy = editedBy;
    }

    public Long getId() {
        return id;
    }

    public Comment getComment() {
        return comment;
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

    public User getEditedBy() {
        return editedBy;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
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

    public void setEditedBy(User editedBy) {
        this.editedBy = editedBy;
    }
}
