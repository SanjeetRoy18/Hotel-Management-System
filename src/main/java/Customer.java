public class Customer {
    private String name;
    private String contact;
    private int roomNumber;

    public Customer(String name, String contact, int roomNumber) {
        this.name = name;
        this.contact = contact;
        this.roomNumber = roomNumber;
    }

    public String getName() { return name; }
    public String getContact() { return contact; }
    public int getRoomNumber() { return roomNumber; }
}