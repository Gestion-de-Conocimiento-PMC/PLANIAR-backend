package com.planiarback.planiar.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(name = "class_id")
    private Long classId;
    
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "due_time")
    private LocalTime dueTime;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "segment_index")
    private Integer segmentIndex;

    @Column(name = "total_segments")
    private Integer totalSegments;

    @Column(name = "working_date")
    private LocalDate workingDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;
    
    @Column(length = 50)
    private String priority; // High, Medium, Low
    
    @Column(name = "estimated_time")
    private Integer estimatedTime; // En minutos
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 50)
    private String type; // Homework, Activity
    
    @Column(name = "state")
    private String state; // Pending, In Progress, Completed
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference // Evita recursión en JSON
    private User user;

    /**
     * @return Long return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return String return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return Long return the classId
     */
    public Long getClassId() {
        return classId;
    }

    /**
     * @param classId the classId to set
     */
    public void setClassId(Long classId) {
        this.classId = classId;
    }

    /**
     * @return LocalDate return the date
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * @param dueDate the dueDate to set
     */
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * @return LocalTime return the dueTime
     */
    public LocalTime getDueTime() {
        return dueTime;
    }

    /**
     * @param dueTime the dueTime to set
     */
    public void setDueTime(LocalTime dueTime) {
        this.dueTime = dueTime;
    }

    /**
     * @return Long return the parentId
     */
    public Long getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    /**
     * @return Integer return the segmentIndex
     */
    public Integer getSegmentIndex() {
        return segmentIndex;
    }

    /**
     * @param segmentIndex the segmentIndex to set
     */
    public void setSegmentIndex(Integer segmentIndex) {
        this.segmentIndex = segmentIndex;
    }

    /**
     * @return Integer return the totalSegments
     */
    public Integer getTotalSegments() {
        return totalSegments;
    }

    /**
     * @param totalSegments the totalSegments to set
     */
    public void setTotalSegments(Integer totalSegments) {
        this.totalSegments = totalSegments;
    }

    /**
     * @return LocalDate return the workingDate
     */
    public LocalDate getWorkingDate() {
        return workingDate;
    }

    /**
     * @param workingDate the workingDate to set
     */
    public void setWorkingDate(LocalDate workingDate) {
        this.workingDate = workingDate;
    }

    /**
     * @return LocalTime return the startTime
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    /**
     * @return LocalTime return the endTime
     */
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * @param endTime the endTime to set
     */
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
    
    /**
     * @return String return the priority
     */
    public String getPriority() {
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(String priority) {
        this.priority = priority;
    }

    /**
     * @return int return the estimatedTime
     */
    public int getEstimatedTime() {
        return estimatedTime;
    }

    /**
     * @param estimatedTime the estimatedTime to set
     */
    public void setEstimatedTime(int estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    /**
     * @return String return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return String return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return String return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @return User return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @param User the user to set
     */
    public void setUser(User user) {
        this.user = user;
    }

}