package com.schoolapp.repository;

import com.schoolapp.model.Class;
import com.schoolapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassRepository extends JpaRepository<Class, UUID> {

    List<Class> findByTeacher(User teacher);

    List<Class> findByTeacherId(UUID teacherId);

    @Query("SELECT c FROM Class c WHERE c.teacher.id = :teacherId ORDER BY c.gradeLevel, c.name")
    List<Class> findByTeacherIdOrderByGradeLevel(@Param("teacherId") UUID teacherId);

    @Query("SELECT c FROM Class c JOIN c.enrollments e WHERE e.student.id = :studentId AND e.isActive = true")
    List<Class> findClassesByStudent(@Param("studentId") UUID studentId);

    @Query("SELECT c FROM Class c WHERE c.gradeLevel = :gradeLevel ORDER BY c.name")
    List<Class> findByGradeLevel(@Param("gradeLevel") Integer gradeLevel);

    @Query("SELECT c FROM Class c WHERE c.subject ILIKE %:subject% ORDER BY c.gradeLevel, c.name")
    List<Class> searchBySubject(@Param("subject") String subject);

    @Query("SELECT c FROM Class c WHERE c.name ILIKE %:name% OR c.description ILIKE %:name% ORDER BY c.gradeLevel, c.name")
    List<Class> searchByNameOrDescription(@Param("name") String name);

    @Query("SELECT COUNT(c) FROM Class c WHERE c.teacher.id = :teacherId")
    long countByTeacherId(@Param("teacherId") UUID teacherId);

    @Query("SELECT c FROM Class c WHERE c.maxStudents > (SELECT COUNT(e) FROM ClassEnrollment e WHERE e.class.id = c.id AND e.isActive = true)")
    List<Class> findClassesWithSpaceAvailable();
}