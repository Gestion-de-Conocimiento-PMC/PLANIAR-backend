package com.planiarback.planiar.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Class {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String title;

    // "0,1,1,0,1,0,0" representa Dom,Lun,Mar,Mie,Jue,Vie,Sab
    @Column(name = "days")
    private String days;
    
    // "18:30,14:00,09:15" - horarios de inicio separados por comas
    @Column(name = "start_times")
    private String startTimes;

    // "20:00,16:30,11:00" - horarios de fin separados por comas
    @Column(name = "end_times")
    private String endTimes;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
    // """,Zair,Tomas,"",Henry,"","" representa los profesores de cada día (Dom,Lun,Mar,Mie,Jue,Vie,Sab)
    @Column(name = "professor")
    private String professor;
    // """,Ml203,W902,"",R290,"","" representa los salones de cada día (Dom,Lun,Mar,Mie,Jue,Vie,Sab)
    @Column(name = "room")
    private String room;
    
    @Column(name = "color", length = 7)
    private String color;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
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
     * @return String return the days
     */
    public String getDays() {
        return days;
    }

    /**
     * @param days the days to set
     */
    public void setDays(String days) {
        this.days = days;
    }

    /**
     * @return String return the startTimes
     */
    public String getStartTimes() {
        return startTimes;
    }

    /**
     * @param startTimes the startTimes to set
     */
    public void setStartTimes(String startTimes) {
        this.startTimes = startTimes;
    }

    /**
     * @return String return the endTimes
     */
    public String getEndTimes() {
        return endTimes;
    }

    /**
     * @param endTimes the endTimes to set
     */
    public void setEndTimes(String endTimes) {
        this.endTimes = endTimes;
    }

    /**
     * @return LocalDate return the startDate
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * @return LocalDate return the endDate
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * @param endDate the endDate to set
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * @return String return the color
     */
    public String getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * @return User return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(User user) {
        this.user = user;
    }

    public String getProfessor() {
        return professor;
    }

    public void setProfessor(String professor) {
        this.professor = professor;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }
}