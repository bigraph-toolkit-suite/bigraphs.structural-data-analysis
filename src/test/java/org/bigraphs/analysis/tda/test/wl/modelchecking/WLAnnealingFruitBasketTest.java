/*
 * Copyright (c) 2026 Bigraph Toolkit Suite Developers
 * Main Developer: Dominik Grzelak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bigraphs.analysis.tda.test.wl.modelchecking;

import static org.bigraphs.framework.core.factory.BigraphFactory.pureBuilder;
import static org.bigraphs.framework.core.factory.BigraphFactory.pureSignatureBuilder;
import static org.bigraphs.framework.simulation.modelchecking.ModelCheckingOptions.transitionOpts;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.bigraphs.analysis.tda.wl.WLAnnealingFrontierStrategy;
import org.bigraphs.framework.core.Bigraph;
import org.bigraphs.framework.core.ControlStatus;
import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.exceptions.InvalidReactionRuleException;
import org.bigraphs.framework.core.exceptions.builder.TypeNotExistsException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.pure.PureBigraphBuilder;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.framework.core.impl.signature.DynamicSignatureBuilder;
import org.bigraphs.framework.core.reactivesystem.ParametricReactionRule;
import org.bigraphs.framework.core.reactivesystem.ReactiveSystemPredicate;
import org.bigraphs.framework.simulation.matching.pure.PureReactiveSystem;
import org.bigraphs.framework.simulation.modelchecking.BigraphModelChecker;
import org.bigraphs.framework.simulation.modelchecking.ModelCheckingOptions;
import org.bigraphs.framework.simulation.modelchecking.PureBigraphModelChecker;
import org.bigraphs.framework.simulation.modelchecking.predicates.SubBigraphMatchPredicate;
import org.bigraphs.testing.BigraphUnitTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Dominik Grzelak
 */
@Disabled
public class WLAnnealingFruitBasketTest implements BigraphUnitTestSupport {

    static final String DUMP_PATH = "src/test/resources/dump/sa-fruits/";

    long startTime = System.currentTimeMillis();
    long finishTime = System.currentTimeMillis();

    @BeforeEach
    void setUp() throws IOException {
        File dump = new File(DUMP_PATH);
        dump.mkdirs();
        FileUtils.cleanDirectory(new File(DUMP_PATH));
        new File(DUMP_PATH + "states/").mkdir();
    }

    @Test
    void simulated_annealing() throws Exception {
        ModelCheckingOptions opts = ModelCheckingOptions.create();
        opts
                .and(transitionOpts()
                        .setMaximumTransitions(100)
                        .setMaximumTime(60)
                        .create()
                )
                .and(ModelCheckingOptions.exportOpts()
                        .setReactionGraphFile(new File(DUMP_PATH + "transition_graph.png"))
                        .setPrintCanonicalStateLabel(false)
                        .setOutputStatesFolder(new File(DUMP_PATH + "states/"))
                        .setFormatsEnabled(List.of(ModelCheckingOptions.ExportOptions.Format.PNG, ModelCheckingOptions.ExportOptions.Format.XMI))
                        .create()
                )
        ;


        PureReactiveSystem rs = new PureReactiveSystem();
        rs.setAgent(createAgent());
        rs.addReactionRule(createRR1());
        rs.addReactionRule(createRR2());
        rs.addReactionRule(createRR3());
        rs.addReactionRule(createRR4());

        PureBigraph goalState = createGoalState();
        toPNG(goalState, "goalstate", DUMP_PATH);
        toPNG(rs.getAgent(), "initstate", DUMP_PATH);
        rs.addPredicate(createPredicate_1());
        rs.addPredicate(createPredicate_2());
        rs.addPredicate(createPredicate_3());
        assert rs.isSimple();

        WLAnnealingFrontierStrategy strategy = new WLAnnealingFrontierStrategy(List.of(goalState), 10, 5, 10, 10);

        PureBigraphModelChecker modelChecker = new PureBigraphModelChecker(
                rs,
                strategy,
//                BigraphModelChecker.SimulationStrategy.Type.BFS,
                opts);
        strategy.withModelChecker(modelChecker);
        modelChecker.setReactiveSystemListener(new BigraphModelChecker.ReactiveSystemListener() {
            @Override
            public void onAllPredicateMatched(Bigraph currentAgent, String label) {
                System.out.println("Match (All)");
                finishTime = System.currentTimeMillis();
                System.out.println("Time elapsed: " + (finishTime - startTime) + " ms");
            }

            @Override
            public void onPredicateMatched(Bigraph currentAgent, ReactiveSystemPredicate predicate) {
                System.out.println("Match " + predicate.getLabel());
                finishTime = System.currentTimeMillis();
                System.out.println("Time elapsed: " + (finishTime - startTime) + " ms");
            }
        });

        startTime = System.currentTimeMillis();
        modelChecker.execute();
        finishTime = System.currentTimeMillis();
        System.out.println("(Final) Time elapsed: " + (finishTime - startTime) + " ms");
    }

    public static DynamicSignature createSignature() {
        DynamicSignatureBuilder defaultBuilder = pureSignatureBuilder();
        defaultBuilder
                .add("User", 1, ControlStatus.ATOMIC)
                .add("Basket", 1)
                .add("Fruit", 1, ControlStatus.ATOMIC)
                .add("Table", 0)
        ;
        return defaultBuilder.create();
    }

    public static PureBigraph createGoalState() throws InvalidConnectionException {
        PureBigraphBuilder<DynamicSignature> builder = pureBuilder(createSignature());
        builder.createOuter("f");
        return builder
                .root()
                .child("User")
                .child("Table").down()
                .child("Basket").down()
                .child("Fruit")
                .child("Fruit")
                .child("Fruit")
                .child("Fruit")
                .create();
    }

    public static PureBigraph createAgent() throws InvalidConnectionException {
        return pureBuilder(createSignature()).root()
                .child("User")
                .child("Table").down()
                .child("Basket")
                .child("Fruit", "f")
                .child("Fruit", "f")
                .child("Fruit", "f")
                .child("Fruit", "f")
                .create();
    }

    public static ParametricReactionRule<PureBigraph> createRR1() throws InvalidReactionRuleException, InvalidConnectionException {
        PureBigraphBuilder<DynamicSignature> b1 = pureBuilder(createSignature());
        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(createSignature());
        b1.root()
                .child("User")
                .child("Table").down()
                .child("Fruit", "f").site()
        ;
        b2.root()
                .child("User", "f")
                .child("Table").down()
                .child("Fruit", "f").site()
        ;
        PureBigraph redex = b1.create();
        PureBigraph reactum = b2.create();
        ParametricReactionRule<PureBigraph> rr = new ParametricReactionRule<>(redex, reactum).withLabel("rr1");
        return rr;
    }

    public static ParametricReactionRule<PureBigraph> createRR2() throws InvalidReactionRuleException, InvalidConnectionException {
        PureBigraphBuilder<DynamicSignature> b1 = pureBuilder(createSignature());
        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(createSignature());
        b1.root()
                .child("User", "f")
                .child("Table").down()
                .child("Fruit", "f").site()
                .child("Basket").down().site()
        ;
        b2.root()
                .child("User", "f")
                .child("Table").down()
                .child("Fruit", "f").site()
                .child("Basket", "f").down().site()
        ;
        PureBigraph redex = b1.create();
        PureBigraph reactum = b2.create();
        ParametricReactionRule<PureBigraph> rr = new ParametricReactionRule<>(redex, reactum).withLabel("rr2");
        return rr;
    }

    public static ParametricReactionRule<PureBigraph> createRR3() throws InvalidReactionRuleException, InvalidConnectionException {
        PureBigraphBuilder<DynamicSignature> b1 = pureBuilder(createSignature());
        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(createSignature());
        b1.root()
                .child("User", "f")
                .child("Table").down()
                .child("Fruit", "f").site()
                .child("Basket", "f").down().site()
        ;
        b2.root()
                .child("User", "f")
                .child("Table").down()
                .site()
                .child("Basket", "f").down().child("Fruit", "f").site()
        ;
        PureBigraph redex = b1.create();
        PureBigraph reactum = b2.create();
        ParametricReactionRule<PureBigraph> rr = new ParametricReactionRule<>(redex, reactum).withLabel("rr3");
        return rr;
    }

    public static ParametricReactionRule<PureBigraph> createRR4() throws InvalidReactionRuleException, InvalidConnectionException {
        PureBigraphBuilder<DynamicSignature> b1 = pureBuilder(createSignature());
        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(createSignature());
        b1.root()
                .child("User", "f")
                .child("Table").down().site()
                .child("Basket", "f").down().child("Fruit", "f").site()
        ;
        b2.createOuter("f");
        b2.root()
                .child("User")
                .child("Table").down().site()
                .child("Basket").down().child("Fruit").site()
        ;
        PureBigraph redex = b1.create();
        PureBigraph reactum = b2.create();
        ParametricReactionRule<PureBigraph> rr = new ParametricReactionRule<>(redex, reactum).withLabel("rr4");
        return rr;
    }

    private static SubBigraphMatchPredicate<PureBigraph> createPredicate_1() throws InvalidConnectionException, TypeNotExistsException {
        PureBigraphBuilder<DynamicSignature> builder = pureBuilder(createSignature());

        builder.root()
                .child("Basket")
                .down().site().child("Fruit").child("Fruit").child("Fruit");
        PureBigraph bigraph = builder.create();
        return SubBigraphMatchPredicate.create(bigraph).withLabel("p1");
    }

    private static SubBigraphMatchPredicate<PureBigraph> createPredicate_2() throws InvalidConnectionException, TypeNotExistsException {
        PureBigraphBuilder<DynamicSignature> builder = pureBuilder(createSignature());

        builder.root()
                .child("Basket")
                .down().site().child("Fruit", "f").child("Fruit").child("Fruit");
        PureBigraph bigraph = builder.create();
        return SubBigraphMatchPredicate.create(bigraph).withLabel("p2");
    }

    private static SubBigraphMatchPredicate<PureBigraph> createPredicate_3() throws InvalidConnectionException, TypeNotExistsException {
        PureBigraphBuilder<DynamicSignature> builder = pureBuilder(createSignature());

        builder.root()
                .child("Basket")
                .down().site().child("Fruit", "f").child("Fruit", "f").child("Fruit");
        PureBigraph bigraph = builder.create();
        return SubBigraphMatchPredicate.create(bigraph).withLabel("p3");
    }
}
