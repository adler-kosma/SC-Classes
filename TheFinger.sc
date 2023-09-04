TheFinger {
	var server, buffer, testSynth, inSynth, array;
	var <synthGroup, fxGroup, <theFingerInBus, masterSynth;
	var noteOnFunc, noteOffFunc;
	var fxAssignments;

	*new {
		^super.new.initTheFinger;
	}

	initTheFinger {
		array = nil!128;
		server = Server.default;
		MIDIClient.init;
		MIDIIn.connectAll;
		fxAssignments = [
			\reverb,
			\dist
		];

		noteOnFunc = MIDIFunc.noteOn({|...args|
			this.addFx(args[1], fxAssignments[args[1] % fxAssignments.size]);
		});
		noteOffFunc = MIDIFunc.noteOff({|...args|
			this.removeFx(args[1]);
		});

		server.waitForBoot {
			TheFinger.buildSynthDefs;
			theFingerInBus = Bus.audio(server, 2);
			synthGroup = Group.new(server);
			fxGroup = Group.after(synthGroup);
			server.sync;
			inSynth = Synth(\in, [\inBus, theFingerInBus, \outBus, 0], synthGroup);
			masterSynth = Synth.after(fxGroup, \master, [\inBus, 0]);
		};
	}

	addFx {|index, fxName|
		if(array[index].isNil, {
			array[index] = Synth(fxName, [\inBus, 0, \outBus, 0], fxGroup, \addToTail);
		}, {
			array[index].set(\gate, 0);
			array[index] = Synth(fxName, [\inBus, 0, \outBus, 0], fxGroup, \addToTail);
		});
	}

	removeFx {|index|
		if(array[index].notNil, {
			array[index].set(\gate, 0);
			array[index] = nil;
		});
	}

	removeAllFx {
		array.do{|f, i|
			if(f.notNil, {
				f.set(\gate, 0);
				array[i] = nil;
			});
		};
	}

	*buildSynthDefs {
		SynthDef(\in, {|inBus, outBus|
			var sig;
			sig = In.ar(inBus, 2);
			Out.ar(outBus, sig);
		}).add;

		SynthDef(\master, {|inBus|
			var sig;
			sig = In.ar(inBus, 2);
			sig = Limiter.ar(sig);
			Out.ar(0, sig);
		}).add;

		SynthDef(\reverb, {|inBus, outBus, gate = 1|
			var sig, env;
			env = EnvGen.kr(Env.asr(0, 1, 0.02), gate, doneAction: 2);
			//sig = NHHall.ar(In.ar(inBus, 2), 3,) * env;
			sig = GVerb.ar(In.ar(inBus, 2).sum, drylevel: 0) * env * 0.55;
			XOut.ar(outBus, 1, sig);
		}).add;

		SynthDef(\dist, {|inBus, outBus, gate = 1, saturation = 30|
			var sig, env, satVal;
			satVal = saturation.lag(0.1).clip(1.0, 99.0);
			env = EnvGen.kr(Env.asr(0, 1, 0.05), gate, doneAction: 2);
			sig = In.ar(inBus, 2);
			sig = (sig * satVal).tanh / (satVal ** 0.6); // formula by James Harkins (satVal can't be 0!)
			sig = sig * env;
			ReplaceOut.ar(outBus, sig);
		}).add;
	}
}