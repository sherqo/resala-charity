# Volunteer

Volunteers, skills, assignments, hours and certificates.

## Features (see `docs/design.md`)

- V1 volunteer CRUD + availability
- V2 skills per volunteer (lookup table)
- V3 assign volunteer to an event/task (skill + date check)
- V4 log hours per assignment
- V5 summaries: total, per event, per month
- V6 certificate at >= 20 hours
- V7 search by skill / availability

## Patterns

- **Iterator** (Phase 2) — `VolunteerCollection` iterates and filters without exposing the list
- Optional second **Strategy** for assignment rules — keep it out of the class diagram

## Tables

`volunteers`, `skills`, `volunteer_skills`, `volunteer_hours`

## Files to create

`VolunteerRepository`, `VolunteerService`, `CertificateGenerator`, then
`VolunteerCollection` (Phase 2).
