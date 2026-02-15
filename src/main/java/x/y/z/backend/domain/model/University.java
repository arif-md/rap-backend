package x.y.z.backend.domain.model;

import java.time.LocalDateTime;

/**
 * University POJO - Domain model representing a university reference entity.
 * Plain Java object without JPA annotations, used with MyBatis.
 */
public class University {

    private Long id;
    private String universityName;
    private String universityCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public University() {
    }

    public University(String universityName, String universityCode) {
        this.universityName = universityName;
        this.universityCode = universityCode;
        this.status = "ACTIVE";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUniversityName() {
        return universityName;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public String getUniversityCode() {
        return universityCode;
    }

    public void setUniversityCode(String universityCode) {
        this.universityCode = universityCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "University{" +
                "id=" + id +
                ", universityName='" + universityName + '\'' +
                ", universityCode='" + universityCode + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
