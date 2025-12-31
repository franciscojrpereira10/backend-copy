package pt.ipleiria.estg.dei.ei.dae.academics.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Timestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "publication_history")
public class PublicationHistory implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @NotNull
    @JoinColumn(name = "publication_id")
    private Publication publication;

    @Column(length = 1000)
    private String oldValue;

    @Column(length = 1000)
    private String newValue;


    @Column(length = 1000)
    private String fieldChanged;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "changed_at", nullable = false)
    private Date changedAt;

    public PublicationHistory() {

    }

    public PublicationHistory(Long id, Publication publication, String oldValue, String newValue, String fieldChanged, User changedBy, Date changedAt) {
        this.id = id;
        this.publication = publication;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.fieldChanged = fieldChanged;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public Long getId() {
        return id;
    }

    public @NotNull Publication getPublication() {
        return publication;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getFieldChanged() {
        return fieldChanged;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public Date getChangedAt() {
        return changedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPublication(@NotNull Publication publication) {
        this.publication = publication;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public void setFieldChanged(String fieldChanged) {
        this.fieldChanged = fieldChanged;
    }

    public void setChangedBy(User changedBy) {
        this.changedBy = changedBy;
    }

    public void setChangedAt(Date changedAt) {
        this.changedAt = changedAt;
    }

    public void changed() {
        this.changedAt = new Date();
    }

}
