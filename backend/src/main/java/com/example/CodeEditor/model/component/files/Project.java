package com.example.CodeEditor.model.component.files;

import com.example.CodeEditor.model.clients.Client;
import jakarta.persistence.*;
import lombok.*;
import net.minidev.json.annotate.JsonIgnore;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "client_id"})})
public class Project implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;
    @Column(nullable = false)
    private String name;
    @ManyToOne
    @JoinColumn(name = "client_id")
    @JsonIgnore
    private Client client;


    @Override
    public String toString() {
        return "Project{" +
                "Id=" + Id +
                ", name='" + name + '\'' +
                '}';
    }
}
