package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {

    def gradle
    def buildFile

    def maryttsVersion = System.properties.maryttsVersion
    def dataDependencyName = 'cmu_time_awb'
    def dataDependency = "org.festvox:$dataDependencyName::ldom@tar.bz2"

    @BeforeSuite
    void setup() {
        def testKitDir = new File(System.properties.testKitDir)

        def projectDir = new File(System.properties.testProjectDir)
        projectDir.mkdirs()
        buildFile = new File(projectDir, 'build.gradle')

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        def pluginClasspath = pluginClasspathResource.readLines()
                .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { new File(it) }

        gradle = GradleRunner.create().withTestKitDir(testKitDir).withProjectDir(projectDir).withPluginClasspath(pluginClasspath)

        // Add the logic under test to the test build
        buildFile.text = """
        plugins {
            id 'de.dfki.mary.voicebuilding-festvox' // transitively applies voicebuilding-data plugin
        }

        repositories {
            ivy {
                url 'http://festvox.org/examples'
                layout 'pattern', {
                    artifact '[module]_[classifier]/packed/[artifact].[ext]'
                }
            }
        }

        dependencies {
            data "$dataDependency"
            runtime group: 'de.dfki.mary', name: 'marytts-common', version: '$maryttsVersion'
        }

        task testPlugins(group: 'Verification') << {
            assert plugins.findPlugin('java')
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-base')
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-data')
        }

        task testConfigurations(group: 'Verification') << {
            assert configurations.data
        }

        task testSourceSets(group: 'Verification') << {
            assert sourceSets.data
        }

        task testDependencies(group: 'Verification') << {
            assert configurations.data.dependencies.find { it.name == "$dataDependencyName" }
        }

        task testProcessDataResources {
            group 'Verification'
            dependsOn processDataResources
            doLast {
                assert fileTree(sourceSets.data.output.resourcesDir).include('*.wav').files
                assert fileTree(sourceSets.data.output.resourcesDir).include('*.lab').files
                assert fileTree(sourceSets.data.output.resourcesDir).include('*.data').files
            }
        }

        text.srcFileName = 'time.data'

        generateAllophones.dependsOn text

        task testWav {
            group 'Verification'
            dependsOn wav
            doLast {
                assert fileTree("\$buildDir/wav").include('*.wav').files
            }
        }

        task testPraatPitchmarker {
            group 'Verification'
            dependsOn praatPitchmarker
            doLast {
                assert fileTree("\$buildDir/pm").include('*.PointProcess').files
                assert fileTree("\$buildDir/pm").include('*.pm').files
            }
        }

        task testMcepMaker {
            group 'Verification'
            dependsOn mcepMaker
            doLast {
                assert fileTree("\$buildDir/mcep").include('*.mcep').files
            }
        }

        task testText {
            group 'Verification'
            dependsOn text
            doLast {
                assert fileTree(buildDir).include('text/*.txt').files
            }
        }

        task testGenerateAllophones {
            group 'Verification'
            dependsOn generateAllophones
            doLast {
                assert fileTree("\$buildDir/prompt_allophones").include('*.xml').files
            }
        }

        task testGeneratePhoneFeatures {
            group 'Verification'
            dependsOn generatePhoneFeatures
            doLast {
                assert fileTree("\$buildDir/phonefeatures").include('*.pfeats').files
            }
        }

        task testGenerateHalfPhoneFeatures {
            group 'Verification'
            dependsOn generateHalfPhoneFeatures
            doLast {
                assert fileTree("\$buildDir/halfphonefeatures").include('*.hpfeats').files
            }
        }

        task testMaryJavaExec(type: JavaExec) {
            classpath sourceSets.main.runtimeClasspath
            main 'marytts.util.PrintSystemProperties'
        }
        """
    }

    @Test
    void testHelp() {
        def result = gradle.withArguments().build()
        println result.output
        assert result.task(':help').outcome == SUCCESS
    }

    @Test
    void testConfigurations() {
        def result = gradle.withArguments('testConfigurations').build()
        println result.output
        assert result.task(':testConfigurations').outcome == SUCCESS
    }

    @Test
    void testSourceSets() {
        def result = gradle.withArguments('testSourceSets').build()
        println result.output
        assert result.task(':testSourceSets').outcome == SUCCESS
    }

    @Test
    void testDependencies() {
        def result = gradle.withArguments('testDependencies').build()
        println result.output
        assert result.task(':testDependencies').outcome == SUCCESS
    }

    @Test
    void testPraatPitchmarker() {
        def result = gradle.withArguments('praatPitchmarker').build()
        println result.output
        assert result.task(':wav').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':praatPitchmarker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testPraatPitchmarker').build()
        println result.output
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':praatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':testPraatPitchmarker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testPraatPitchmarker'])
    void testMcepMaker() {
        def result = gradle.withArguments('mcepMaker').build()
        println result.output
        assert result.task(':mcepMaker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testMcepMaker').build()
        println result.output
        assert result.task(':mcepMaker').outcome == UP_TO_DATE
        assert result.task(':testMcepMaker').outcome == SUCCESS
    }

    @Test
    void testGenerateAllophones() {
        def result = gradle.withArguments('generateAllophones').build()
        println result.output
        assert result.task(':text').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':generateAllophones').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testGenerateAllophones').build()
        println result.output
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':testGenerateAllophones').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testGenerateAllophones'])
    void testGeneratePhoneFeatures() {
        def result = gradle.withArguments('generatePhoneFeatures').build()
        println result.output
        assert result.task(':generatePhoneFeatures').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testGeneratePhoneFeatures').build()
        println result.output
        assert result.task(':generatePhoneFeatures').outcome == UP_TO_DATE
        assert result.task(':testGeneratePhoneFeatures').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testGenerateAllophones'])
    void testGenerateHalfPhoneFeatures() {
        def result = gradle.withArguments('generateHalfPhoneFeatures').build()
        println result.output
        assert result.task(':generateHalfPhoneFeatures').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testGenerateHalfPhoneFeatures').build()
        println result.output
        assert result.task(':generateHalfPhoneFeatures').outcome == UP_TO_DATE
        assert result.task(':testGenerateHalfPhoneFeatures').outcome == SUCCESS
    }

    @Test
    void testMaryJavaExec() {
        def result = gradle.withArguments('testMaryJavaExec').build()
        println result.output
        assert result.task(':testMaryJavaExec').outcome == SUCCESS
    }
}
