package searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;
    @Column(name = "path", nullable = false, columnDefinition = "TEXT")
    private String path;
    @Column(name = "code", nullable = false)
    private int code;
    @Lob
    @Column(name = "content", nullable = false)
    private String content;
}
