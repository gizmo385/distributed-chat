import java.util.Set;
import java.util.TreeSet;

public class Room {
    // Global variables
    private static int globalRoomIdCounter = 0;

    // Information about the room
    private final int id;
    private Set<Integer> users;

    public Room() {
        this.id = globalRoomIdCounter;
        globalRoomIdCounter++;

        this.users = new TreeSet<>();
    }

    public void addUser(int userId) {
        this.users.add(userId);
    }

    public Set<Integer> getUsers() {
        return this.users;
    }

    public int getId() {
        return this.id;
    }

    public boolean equals(Object other) {
        if( other instanceof Room ) {
            return id == ((Room)other).getId();
        }

        return false;
    }

    public int hashCode() {
        return this.id;
    }
}
