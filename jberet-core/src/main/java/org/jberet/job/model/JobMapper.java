package org.jberet.job.model;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ObjectFactory;

import java.util.List;

@Mapper(uses = JobMapper.JobMapperFactory.class, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface JobMapper {
    Job job(Job job);

    List<Properties> properties(List<Properties> properties);

    Properties properties(Properties properties);

    Listeners listeners(Listeners listeners);

    List<Transition> transitions(List<Transition> transitions);

    Transition transition(Transition transition);

    List<JobElement> jobElements(List<JobElement> jobElements);

    @Mapping(target = "transitionElements", ignore = true)
    JobElement jobElement(JobElement jobElement);

    Step step(Step step);

    List<RefArtifact> refArtifacts(List<RefArtifact> refArtifacts);

    RefArtifact refArtifact(RefArtifact refArtifact);

    Chunk chunk(Chunk chunk);

    ExceptionClassFilter exceptionClassFilter(ExceptionClassFilter exceptionClassFilter);

    Partition partition(Partition partition);

    PartitionPlan partitionPlan(PartitionPlan partitionPlan);

    Decision decision(Decision decision);

    List<Flow> flows(List<Flow> flows);

    Flow flow(Flow flow);

    Split split(Split split);

    class JobMapperFactory {
        private static final JobMapper jobMapper = new JobMapperImpl();

        @ObjectFactory
        static Job createJob(Job job) {
            return new Job(job.getId());
        }

        @ObjectFactory
        static Transition createTransition(Transition transition) {
            if (transition instanceof Transition.Next) {
                Transition.Next<Object> next = new Transition.Next<>(transition.getOn());
                next.setTo(((Transition.Next<?>) transition).getTo());
                return next;
            }

            if (transition instanceof Transition.Stop) {
                Transition.Stop<Object> stop = new Transition.Stop<>(transition.getOn(), ((Transition.Stop<?>) transition).getRestart());
                stop.setExitStatus(((Transition.Termination<?>) transition).getExitStatus());
                return stop;
            }

            if (transition instanceof Transition.Fail) {
                final Transition.Fail<Object> fail = new Transition.Fail<>(transition.getOn());
                fail.setExitStatus(((Transition.Termination<?>) transition).getExitStatus());
                return fail;
            }

            if (transition instanceof Transition.End) {
                final Transition.End<Object> end = new Transition.End<>(transition.getOn());
                end.setExitStatus(((Transition.Termination<?>) transition).getExitStatus());
                return end;
            }

            throw new UnsupportedOperationException();
        }

        @ObjectFactory
        static JobElement createJobElement(JobElement jobElement) {
            if (jobElement instanceof Step) {
                return jobMapper.step((Step) jobElement);
            }

            if (jobElement instanceof Decision) {
                return jobMapper.decision((Decision) jobElement);
            }

            if (jobElement instanceof Flow) {
                return jobMapper.flow((Flow) jobElement);
            }

            if (jobElement instanceof Split) {
                return jobMapper.split((Split) jobElement);
            }

            throw new UnsupportedOperationException();
        }

        @ObjectFactory
        static RefArtifact createRefArtifact(RefArtifact refArtifact) {
            return new RefArtifact(refArtifact.getRef());
        }

        @ObjectFactory
        static Step createStep(Step step) {
            return new Step(step.getId());
        }

        @ObjectFactory
        static Decision createDecision(Decision decision) {
            return new Decision(decision.getId(), decision.getRef());
        }

        @ObjectFactory
        static Flow createFlow(Flow flow) {
            return new Flow(flow.getId());
        }

        @ObjectFactory
        static Split createSplit(Split split) {
            return new Split(split.getId());
        }
    }
}
