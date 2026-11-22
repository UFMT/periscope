package br.ufmt.periscope.api.dto;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public record ProjectDTO(
        String id,
        String title,
        String description,
        Boolean isPublic,
        Instant createdAt,
        Instant updateAt,
        String ownerId,
        String ownerName,
        long patentCount,
        List<String> observerIds,
        List<UserSummaryDTO> observers
) {
    public static ProjectDTO from(Project project, long patentCount) {
        if (project == null) {
            return null;
        }
        User owner = project.getOwner();
        List<UserSummaryDTO> observerSummaries = project.getObservers() == null
                ? List.of()
                : project.getObservers().stream()
                .filter(u -> u != null && u.getId() != null)
                .map(UserSummaryDTO::from)
                .toList();
        List<String> observerIds = observerSummaries.stream()
                .map(UserSummaryDTO::id)
                .toList();
        return new ProjectDTO(
                project.getId() != null ? project.getId().toString() : null,
                project.getTitle(),
                project.getDescription(),
                project.getIsPublic(),
                toInstant(project.getCreatedAt()),
                toInstant(project.getUpdateAt()),
                owner != null && owner.getId() != null ? owner.getId().toString() : null,
                owner != null ? owner.toString() : null,
                patentCount,
                observerIds,
                observerSummaries
        );
    }

    public static ProjectDTO from(Project project) {
        int count = project.getPatents() != null ? project.getPatents().size() : 0;
        return from(project, count);
    }

    private static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
