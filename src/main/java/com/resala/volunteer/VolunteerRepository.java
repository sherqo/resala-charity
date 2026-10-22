package com.resala.volunteer;

import com.resala.core.JdbcRepository;
import com.resala.model.Volunteer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class VolunteerRepository extends JdbcRepository<Volunteer> {

    private static final String SELECT_VOLUNTEER =
            "SELECT v.id, v.name, v.email, v.phone, v.available, "
                    + "COALESCE((SELECT SUM(h.hours) FROM volunteer_hours h "
                    + "             WHERE h.volunteer_id = v.id), 0) AS hours "
                    + "FROM volunteers v";
    public VolunteerRepository(){
        super();
    }
    public VolunteerRepository(Connection conn){
        super(conn);
    }

    @Override
    public long insert(Volunteer v) {
        String sql = """
                INSERT INTO volunteers (name, email, phone, available)
                VALUES (?, ?, ?, ?)
                """;
        long id = insertReturnKey(sql,
                v.getName(),
                v.getEmail(),
                v.getPhone(),
                flag(v.isAvailable())
                );

        v.setId(id);
        return id;
    }

    @Override
    public boolean update(Volunteer v) {
        String sql = """
                UPDATE volunteers
                   SET name = ?, email = ?, phone = ?, available = ?
                 WHERE id = ?
                """;
        return execute(sql, v.getName(), v.getEmail(), v.getPhone(), flag(v.isAvailable()), v.getId()) > 0;
    }


    @Override
    public boolean deleteById(long id) {
        return execute("DELETE FROM volunteers WHERE id = ?", id) > 0;
    }

    @Override
    public Optional<Volunteer> findById(long id) {
        return queryOne(SELECT_VOLUNTEER + " WHERE v.id = ?",
                VolunteerRepository::mapRow, id);
    }

    @Override
    public List<Volunteer> findAll() {
        return query(SELECT_VOLUNTEER + " ORDER BY v.name",
                VolunteerRepository::mapRow);
    }

    public boolean addSkill(long vid, long sid){
        return execute(
                "INSERT OR IGNORE INTO volunteer_skills (volunteer_id, skill_id) VALUES (?, ?)", vid, sid
        ) > 0;
    }

    public boolean removeSkill(long vid, long sid){
        return execute(
                "DELETE FROM volunteer_skills WHERE volunteer_id = ? AND skill_id = ?", vid, sid
        ) > 0;
    }

    public List<String> findSkills(long vid){
        String sql = """
                SELECT s.name FROM skills s
                JOIN volunteer_skills vs ON vs.skill_id = s.id
                WHERE vs.volunteer_id = ?
                ORDER BY s.name
                """;
        return query(sql, rs -> str(rs, "name"), vid);
    }

    public List<Volunteer> findBySkill(long sid){
        String sql = SELECT_VOLUNTEER
                + " JOIN volunteer_skills vs ON vs.volunteer_id = v.id"
                + " WHERE vs.skill_id = ?"
                + " ORDER BY v.name";
        return query(sql, VolunteerRepository::mapRow, sid);
    }

    public long logHours(long vid, double hours, Long eventId){
        return logHours(vid, hours, eventId, null);
    }

    public long logHours(long vid, double hours, Long eventId, LocalDate loggedOn){
        return insertReturnKey("""
                INSERT INTO volunteer_hours (volunteer_id, event_id, hours, logged_on)
                VALUES (?, ?, ?, ?)
                """, vid, eventId, hours, (loggedOn == null ? LocalDate.now() : loggedOn).toString());
    }
    public double getTotalHours(long vid) {
        return sumHours(
                "SELECT COALESCE(SUM(hours), 0) AS total FROM volunteer_hours WHERE volunteer_id = ?",
                vid);
    }

    public double getTotalHoursForEvent(long vid, long eventId) {
        return sumHours(
                "SELECT COALESCE(SUM(hours), 0) AS total FROM volunteer_hours "
                        + "WHERE volunteer_id = ? AND event_id = ?",
                vid, eventId);
    }

    public double getTotalHoursInPeriod(long vid, LocalDate from, LocalDate to) {
        return sumHours(
                "SELECT COALESCE(SUM(hours), 0) AS total FROM volunteer_hours "
                        + "WHERE volunteer_id = ? AND logged_on BETWEEN ? AND ?",
                vid, from.toString(), to.toString());
    }

    public List<Volunteer> findByAvailability(boolean available){
        return query(SELECT_VOLUNTEER + " WHERE v.available = ? ORDER BY v.name",
                VolunteerRepository::mapRow, flag(available));
    }

    public List<Volunteer> searchByName(String fragment){
        return query(SELECT_VOLUNTEER + " WHERE v.name LIKE ? ORDER BY v.name",
                VolunteerRepository::mapRow, "%" + fragment + "%");
    }

    public long count(){
        Optional<Long> total = queryOne(
                "SELECT COUNT(*) AS total FROM volunteers", rs -> rs.getLong("total")
        );
        return total.orElse(0L);
    }

    private double sumHours(String sql, Object... params){
        Optional<Double> total = queryOne(sql, rs -> rs.getDouble("total"), params);
        return total.orElse(0.0);
    }

    private static Volunteer mapRow(ResultSet rs) throws SQLException {
        return new Volunteer(
                num(rs, "id"),
                str(rs, "name"),
                str(rs, "email"),
                str(rs, "phone"),
                num(rs, "available") != 0,
                dbl(rs, "hours"));
    }

    private static int flag(boolean value) {
        return value ? 1 : 0;
    }
}
