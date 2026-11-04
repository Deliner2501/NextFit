package com.nextfit.ui.fx;

import com.nextfit.model.Member;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Round-cornered photo of a member. When the member has no photo (or the file is
 * missing) it shows the initials, so every screen keeps the same shape.
 */
public final class MemberAvatar {

    private MemberAvatar() {
    }

    /**
     * Creates the avatar.
     *
     * @param member the member
     * @param size   the side in logical pixels
     * @return the node to place on screen
     */
    public static Node of(Member member, double size) {
        StackPane holder = new StackPane();
        holder.setMinSize(size, size);
        holder.setPrefSize(size, size);
        holder.setMaxSize(size, size);
        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(size * 0.22);
        clip.setArcHeight(size * 0.22);
        holder.setClip(clip);

        Image photo = load(member.getPhotoPath());
        if (photo != null) {
            ImageView view = new ImageView(photo);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setPreserveRatio(false);
            view.setSmooth(true);
            holder.getChildren().add(view);
        } else {
            Label initials = new Label(initialsOf(member.getFullName()));
            initials.getStyleClass().add("nf-avatar-initials");
            initials.setStyle("-fx-font-size: " + Math.round(size * 0.36) + "px;");
            holder.getStyleClass().add("nf-avatar");
            holder.getChildren().add(initials);
        }
        return holder;
    }

    private static Image load(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) {
            return null;
        }
        Path file = Path.of(photoPath);
        return Files.isRegularFile(file) ? new Image(file.toUri().toString(), false) : null;
    }

    private static String initialsOf(String fullName) {
        StringBuilder result = new StringBuilder();
        for (String part : fullName.trim().split("\\s+")) {
            if (!part.isEmpty() && result.length() < 2) {
                result.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return result.toString();
    }
}
