package pt.ipleiria.estg.dei.ei.dae.academics.entities;

import pt.ipleiria.estg.dei.ei.dae.academics.enums.Role;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
@NamedQueries({
        @NamedQuery(
                name = "getAllUsers",
                query = "SELECT u FROM User u"),
        @NamedQuery(
                name = "getUserByName",
                query = "SELECT u FROM User u WHERE u.username = :username"),
        @NamedQuery(
                name = "getUserByEmail",
                query = "SELECT u FROM User u WHERE u.email = :email"),
        @NamedQuery(
                name = "getUsersByRole",
                query = "SELECT u FROM User u WHERE u.role = :role"),
        @NamedQuery(
                name = "getUsersByStatus",
                query = "SELECT u FROM User u WHERE u.status = :status"),
        @NamedQuery(
                name = "User.findByEmail",
                query = "SELECT u FROM User u WHERE u.email = :email")
})

public class User implements Serializable {

    //esta acabado (em principio)
    //IDS NAS CLASSES TÊM DE SER LONG COM (L) MAIUSCULO

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    @NotNull
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    @NotNull
    @Email
    private String email;

    @Column(nullable = false)
    @NotNull
    private String password;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role; // COLABORADOR, RESPONSAVEL, ADMINISTRADOR

    @Column(name = "email_notifications", nullable = false)
    private boolean emailNotifications = true;

    @Column(name = "preferred_language", length = 5, nullable = false)
    private String preferredLanguage = "pt";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status; // ATIVO, SUSPENSO

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_login")
    private Date lastLogin;

    @OneToMany(mappedBy = "uploadedBy", fetch = FetchType.LAZY)
    private List<Publication> publications;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_tag_subscriptions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> subscribedTags;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Rating> ratings;  //cascade.remove faz com que quando se elemina um user, os seus ratings, comments e etc também são eliminados (colocar em tudo o que for desse genero)

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Activity> activities;

    public User() {
        this.publications = new ArrayList<>();
        this.ratings = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.subscribedTags = new ArrayList<>();
        this.notifications = new ArrayList<>();
        this.activities = new ArrayList<>();
        this.status = UserStatus.ACTIVE;
        this.createdAt = new Date();
    }

    public User(String username, String email, String password, Role role) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public @NotNull String getUsername() {
        return username;
    }

    public @NotNull @Email String getEmail() {
        return email;
    }

    public @NotNull String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public List<Publication> getPublications() {
        return publications;
    }

    public List<Tag> getSubscribedTags() {
        return subscribedTags;
    }

    public List<Rating> getRatings() {
        return ratings;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public List<Activity> getActivities() {
        return activities;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(@NotNull String username) {
        this.username = username;
    }

    public void setEmail(@NotNull @Email String email) {
        this.email = email;
    }

    public void setPassword(@NotNull String password) {
        this.password = password;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void setPublications(List<Publication> publications) {
        this.publications = publications;
    }

    public void setSubscribedTags(List<Tag> subscribedTags) {
        this.subscribedTags = subscribedTags;
    }

    public void setRatings(List<Rating> ratings) {
        this.ratings = ratings;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    public void setActivities(List<Activity> activities) {
        this.activities = activities;
    }

    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    // conveniência para o EP43
    public String getMainRole() {
        return role != null ? role.name() : null;
    }


    //metodos auxiliares

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isSuspended() {
        return this.status == UserStatus.INACTIVE;
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    public boolean isManager() {
        return this.role == Role.MANAGER;
    }

    public boolean isContributor() {
        return this.role == Role.CONTRIBUTOR;
    }

    public boolean canEdit(User resourceOwner) {
        return this.equals(resourceOwner) || isManager() || isAdmin();
    }

    public void subscribeTag(Tag tag) {
        if (!subscribedTags.contains(tag)) {
            subscribedTags.add(tag);
            tag.getSubscribers().add(this);
        }
    }

    public void unsubscribeTag(Tag tag) {
        if (subscribedTags.contains(tag)) {
            subscribedTags.remove(tag);
            tag.getSubscribers().remove(this);
        }
    }

    // util para ids autogerados pois o hashCode pode ser calculado com o id a null
    // assim garantimos que o id é sempre o mesmo

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}

