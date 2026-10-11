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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.io.FileUtils;
import org.bigraphs.analysis.tda.test.DemoBigraphProvider;
import org.bigraphs.analysis.tda.wl.WLAnnealingFrontierStrategy;
import org.bigraphs.framework.converter.dot.DOTReactionGraphExporter;
import org.bigraphs.framework.core.exceptions.InvalidReactionRuleException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.pure.PureBigraphBuilder;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.framework.core.impl.signature.DynamicSignatureBuilder;
import org.bigraphs.framework.core.reactivesystem.ParametricReactionRule;
import org.bigraphs.framework.core.reactivesystem.ReactionGraph;
import org.bigraphs.framework.simulation.matching.pure.PureReactiveSystem;
import org.bigraphs.framework.simulation.modelchecking.BigraphModelChecker;
import org.bigraphs.framework.simulation.modelchecking.ModelCheckingOptions;
import org.bigraphs.framework.simulation.modelchecking.PureBigraphModelChecker;
import org.bigraphs.framework.simulation.modelchecking.predicates.SubBigraphMatchPredicate;
import org.bigraphs.testing.BigraphUnitTestSupport;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Dominik Grzelak
 */
@Disabled
public class WLAnnealingBigraphGenerationTest implements BigraphUnitTestSupport, BigraphModelChecker.ReactiveSystemListener<PureBigraph> {

    static final String DUMP_PATH = "src/test/resources/dump/generative-bigraphs/";

    private static final DemoBigraphProvider demoModelProvider = new DemoBigraphProvider();

    long startTime = System.currentTimeMillis();
    long finishTime = System.currentTimeMillis();

    private PureBigraphModelChecker modelChecker;

    @BeforeEach
    void setUp() throws IOException {
        File dump = new File(DUMP_PATH);
        dump.mkdirs();
        FileUtils.cleanDirectory(new File(DUMP_PATH));
        new File(DUMP_PATH + "states/").mkdir();
        new File(DUMP_PATH + "rules/").mkdir();
    }

    @Test
    void simulated_annealing() throws Exception {
        PureReactiveSystem rs = new PureReactiveSystem();
        ModelCheckingOptions opts = opts();

        int n = 3;

        PureBigraph agent = createAgent();
        rs.setAgent(agent);
        toPNG(rs.getAgent(), "initstate", DUMP_PATH);

        PureBigraph goalState = createGoalState(n, 0);
        toPNG(goalState, "goalstate", DUMP_PATH);

//        BigraphIsoPredicate<PureBigraph> predicate = BigraphIsoPredicate.create(goalState);
        SubBigraphMatchPredicate<PureBigraph> predicate = SubBigraphMatchPredicate.create(goalState);
        rs.addPredicate(predicate);
        toPNG(predicate.getBigraph(), "predicate", DUMP_PATH);

        List<ParametricReactionRule<PureBigraph>> ruleSetV1 = createRuleSet(buildSignatureFromNodes(goalState));
        ruleSetV1.forEach(rr -> {
            toPNG(rr.getRedex(), rr.getLabel() + "_LHS", DUMP_PATH + "rules/");
            toPNG(rr.getReactum(), rr.getLabel() + "_RHS", DUMP_PATH + "rules/");
            try {
                rs.addReactionRule(rr);
            } catch (InvalidReactionRuleException e) {
                throw new RuntimeException(e);
            }
        });


        assert rs.isSimple();

        WLAnnealingFrontierStrategy strategy = new WLAnnealingFrontierStrategy(
                List.of(goalState), 100, 10, 2, n
        );
        strategy.setEnergyEps(0.1);
        strategy.setDefaultFilter();
        modelChecker = new PureBigraphModelChecker(
                rs,
                strategy,
//                BigraphModelChecker.SimulationStrategy.Type.BFS,
//                BigraphModelChecker.SimulationStrategy.Type.DFS,
                opts);
        strategy.withModelChecker(modelChecker);
        modelChecker.setReactiveSystemListener(this);
        modelChecker.execute();

        ReactionGraph<PureBigraph> rg = modelChecker.getReactionGraph();
        DOTReactionGraphExporter exporter = new DOTReactionGraphExporter();
        exporter.toOutputStream(rg, new FileOutputStream(DUMP_PATH + "/reaction-graph-generative-bigraphs.dot"));
    }

    @Override
    public void onAllPredicateMatched(PureBigraph currentAgent, String label) {
        System.out.println("Solution found");
        System.out.println(label);
        finishTime = System.currentTimeMillis();
        System.out.println("Time elapsed: " + (finishTime - startTime) + " ms");
        ReactionGraph<PureBigraph> rg = modelChecker.getReactionGraph();
        DOTReactionGraphExporter exporter = new DOTReactionGraphExporter();
        try {
            exporter.toOutputStream(rg, new FileOutputStream(DUMP_PATH + "/reaction-graph-generative-bigraphs.dot"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        modelChecker.getModelCheckingStrategy().requestStop();
    }

    private static @NonNull ModelCheckingOptions opts() {
        ModelCheckingOptions opts = ModelCheckingOptions.create();
        opts
                .and(transitionOpts()
                        .setMaximumTransitions(10000)
                        .setMaximumTime(600)
                        .create()
                )
                .and(ModelCheckingOptions.exportOpts()
//                        .setReactionGraphFile(new File(DUMP_PATH + "transition_graph.png"))
                        .setPrintCanonicalStateLabel(false)
                        .setOutputStatesFolder(new File(DUMP_PATH + "states/"))
                        .setFormatsEnabled(List.of(ModelCheckingOptions.ExportOptions.Format.PNG, ModelCheckingOptions.ExportOptions.Format.XMI))
                        .create()
                )
        ;
        return opts;
    }

    public static DynamicSignature sig() {
//        return demoModelProvider.createABCSignatureArityZero();
        return demoModelProvider.createAlphabetSignature();
    }

    public static PureBigraph createGoalState(int n, int s) {
//        return demoModelProvider.getRandomBigraphSingleRoot(n, s, sig());
        return demoModelProvider.getRandomPlaceGraphSingleRoot(n, s, sig());
    }

    public static PureBigraph createAgent() {
        PureBigraphBuilder<DynamicSignature> b = pureBuilder(sig());
        b.root();
        return b.create();
    }

    private static List<ParametricReactionRule<PureBigraph>> createRuleSet(DynamicSignature signature) {
        // for every used control we create an elementary rule that creates an atom: e -> K (control space)
        List<ParametricReactionRule<PureBigraph>> ruleSet = new ArrayList<>();
        Supplier<String> rrLbls = ruleNameSupplier();
        signature.getControls().forEach(type -> {
            try {
                PureBigraphBuilder<DynamicSignature> bRedex = pureBuilder(signature);
                PureBigraphBuilder<DynamicSignature> bReactum = pureBuilder(signature);
                bRedex.root();
                bReactum.root().child(type);

                PureBigraph redex = bRedex.create();
                PureBigraph reactum = bReactum.create();
                ParametricReactionRule<PureBigraph> rr = new ParametricReactionRule<>(redex, reactum);
                rr.withLabel(rrLbls.get());
                ruleSet.add(rr);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return ruleSet;
    }

    private static Supplier<String> ruleNameSupplier() {
        return new Supplier<>() {
            private int id = 0;

            @Override
            public String get() {
                return "rr_" + id++;
            }
        };
    }

    private DynamicSignature buildSignatureFromNodes(PureBigraph bigraph) {
        DynamicSignatureBuilder sb = pureSignatureBuilder();
        bigraph.getNodes().forEach(x -> {
            sb.add(x.getControl());
        });
        return sb.create();
    }
}
