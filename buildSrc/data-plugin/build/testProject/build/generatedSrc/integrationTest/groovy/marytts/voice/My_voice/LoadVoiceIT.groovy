package marytts.voice.My_voice

import marytts.LocalMaryInterface
import marytts.datatypes.MaryDataType
import marytts.unitselection.UnitSelectionVoice
import marytts.util.dom.DomUtils

import org.testng.annotations.*

public class LoadVoiceIT {

    LocalMaryInterface mary
    Config config

    @BeforeMethod
    void setup() {
        mary = new LocalMaryInterface()
        config = new Config()
    }

    @Test
    void canLoadVoice() {
        def voice = new UnitSelectionVoice(config.name, null)
        assert voice
    }

    @Test
    void canSetVoice() {
        mary.voice = config.name
        assert config.name == mary.voice
    }

    @Test
    void canProcessTextToTargetfeatures() {
        mary.locale = config.locale
        mary.outputType = MaryDataType.TARGETFEATURES
        def input = MaryDataType.getExampleText(MaryDataType.TEXT, config.locale)
        def output = mary.generateText(input)
        assert output
    }

    @Test
    void canProcessTextToSpeech() {
        def mary = new LocalMaryInterface()
        def config = new Config()
        mary.voice = config.name
        def input = MaryDataType.getExampleText(MaryDataType.TEXT, config.locale)
        def output = mary.generateAudio(input)
        assert output
    }

    @Test
    void canProcessTokensToTargetfeatures() {
        mary.locale = config.locale
        mary.inputType = MaryDataType.TOKENS
        mary.outputType = MaryDataType.TARGETFEATURES
        def example = MaryDataType.getExampleText(MaryDataType.TOKENS, config.locale)
        def input = DomUtils.parseDocument(example)
        def output = mary.generateText(input)
        assert output
    }

    @Test
    void canProcessTokensToSpeech() {
        mary.locale = config.locale
        mary.inputType = MaryDataType.TOKENS
        def example = MaryDataType.getExampleText(MaryDataType.TOKENS, config.locale)
        def input = DomUtils.parseDocument(example)
        def output = mary.generateAudio(input)
        assert output
    }
}
