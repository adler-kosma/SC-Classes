AdelesSynths {
	var server;
	var synth, buffers; // kanske lägga in buffers i Dictionary med taggar (tags) kolla: PathName ex (.isSoundFile)
	//classvar
	*new { //klassmetod: ex. a= AdelesSynths.new
		^super.new.initAdelesSynths;
	}

	initAdelesSynths { //instansmetod. på redan skapade instanser av en klass ex. a.playSynth;
		server = Server.default;
		server.waitForBoot{
			AdelesSynths.sendSynthDefs;
			server.sync;
			AdelesSynths.allocBuffers;
		};
	}

	playSynth {|name = \sine, freq = 300, amp = 0.1| // defaultvärde
		synth = Synth(name, [\freq, freq, \amp, amp]); // för att kunna stoppa en synth måste en sätta Synth i en variabel
	}

	stopSynth {
		synth.set(\gate, 0);
	}

	*sendSynthDefs { //klassmetoder
		SynthDef(\sine, {|freq, amp, gate = 1|
			var env, sig;
			env = EnvGen.kr(Env.asr(), gate, doneAction: 2);
			sig = SinOsc.ar(freq, 0, amp * env);
			Out.ar(0, sig!2);
		}).add;

		SynthDef(\saw, {|freq, amp, gate = 1|
			var env, sig;
			env = EnvGen.kr(Env.asr(), gate, doneAction: 2);
			sig = Saw.ar(freq, amp * env);
			Out.ar(0, sig!2);
		}).add;
	}
	*allocBuffer{
		Buffer.read();
	}
}