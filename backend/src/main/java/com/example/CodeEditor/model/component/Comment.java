package com.example.CodeEditor.model.component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Data
@ToString
@AllArgsConstructor
@Builder
public class Comment implements Serializable {
    private String editorName;
    private String editorEmail;
    private Integer start;
    private Integer end;
    private LocalDate date;
    private String content;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Comment comment = (Comment) object;
        return Objects.equals(editorName, comment.editorName)
                && Objects.equals(editorEmail, comment.editorEmail)
                && Objects.equals(start, comment.start)
                && Objects.equals(end, comment.end)
                && Objects.equals(date, comment.date)
                && Objects.equals(content, comment.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(editorName, editorEmail, start, end, date, content);
    }
}
