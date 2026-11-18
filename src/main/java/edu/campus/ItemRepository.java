package edu.campus;

import java.sql.SQLException;
import java.util.List;
import static edu.campus.Item.*;

public interface ItemRepository {
    long add(Draft draft) throws SQLException;
    void edit(long id, Draft draft) throws SQLException;
    List<Item> list() throws SQLException;
    Details details(long id) throws SQLException;
    void claim(long id, Claim claim) throws SQLException;
    void returnItem(long id, String returnedAt) throws SQLException;
}
