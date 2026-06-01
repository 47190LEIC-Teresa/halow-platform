package backend.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "job_scheduler_state")
class JobSchedulerState(

    @Id
    @Column(name = "id", nullable = false)
    var id: Long = 1,

    @Column(name = "last_served_username")
    var lastServedUsername: String? = null
)