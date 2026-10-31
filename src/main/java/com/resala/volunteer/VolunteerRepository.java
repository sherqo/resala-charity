package com.resala.volunteer;

import com.resala.model.Skill;
import com.resala.model.Volunteer;
import com.resala.model.VolunteerEventAssignment;
import java.util.List;

public interface VolunteerRepository {
  Volunteer save(Volunteer volunteer);
  Volunteer findById(int id);
  List<Volunteer> findAll();
  void delete(int id);
  void setAvailability(int id, boolean available);
  List<Volunteer> findBySkill(int skillId);
  List<Volunteer> findAvailable();

  List<Skill> findAllSkills();
  Skill findSkillById(int skillId);
  Skill findSkillByName(String name);
  Skill saveSkill(Skill skill);
  boolean isSkillInUse(int skillId);
  void deleteSkill(int skillId);
  List<Skill> findSkillsForVolunteer(int volunteerId);
  void addSkill(int volunteerId, int skillId);
  void removeSkill(int volunteerId, int skillId);

  VolunteerEventAssignment saveAssignment(VolunteerEventAssignment assignment);
  VolunteerEventAssignment findAssignmentById(int id);
  List<VolunteerEventAssignment> findAssignments(int volunteerId);
  boolean hasAssignment(int volunteerId, int eventId);
}
